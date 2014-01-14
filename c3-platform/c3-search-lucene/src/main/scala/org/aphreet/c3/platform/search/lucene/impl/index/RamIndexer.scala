/**
 * Copyright (c) 2010, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.aphreet.c3.platform.search.lucene.impl.index

import akka.actor._
import collection.JavaConversions._
import java.io.StringReader
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.{IndexWriterConfig, IndexWriter}
import org.apache.lucene.store.{RAMDirectory, Directory}
import org.aphreet.c3.platform.common.msg.{DestroyMsgReply, DestroyMsg}
import org.aphreet.c3.platform.common.{ActorRefHolder, Constants, Logger}
import org.aphreet.c3.platform.resource.{Metadata, Resource}
import org.aphreet.c3.platform.search.lucene.impl.SearchManagerInternal.LUCENE_VERSION
import org.aphreet.c3.platform.search.lucene.impl.common.Fields._
import org.aphreet.c3.platform.search.lucene.impl.common.LanguageGuesserUtil
import org.aphreet.c3.platform.search.lucene.impl.index.extractor.ExtractedDocument
import org.aphreet.c3.platform.search.lucene.{HandleFieldListMsg, SearchConfigurationManager}
import scala.util.control.Exception._


class RamIndexer(val actorSystem: ActorRefFactory,
                 val fileIndexer: FileIndexer,
                 val configurationManager:SearchConfigurationManager, num: Int,
                 var extractDocumentContent:Boolean,
                 var textExtractor:TextExtractor) extends ActorRefHolder {

  val log = Logger(getClass)

  var directory: Directory = null

  val async = actorSystem.actorOf(Props.create(classOf[RamIndexerActor], this))

  class RamIndexerActor extends Actor {

    var lastDocumentTime: Long = System.currentTimeMillis

    val languageGuesser = LanguageGuesserUtil.createGuesser

    var maxDocsCount: Int = 100

    var writer: IndexWriter = null

    {
      createNewWriter()
    }

    def createNewWriter() {

      val oldWriter = writer
      val oldDirectory = directory

      directory = new RAMDirectory

      writer = new IndexWriter(directory, new IndexWriterConfig(LUCENE_VERSION, new StandardAnalyzer(LUCENE_VERSION)))

      if (oldWriter != null) {
        oldWriter.close()
        fileIndexer.mergeIndex(oldDirectory)
      }
    }

    def receive = {
      case IndexMsg(resource) => {
        handling(classOf[Throwable]).by(e => {
          log.warn(num + ": Failed to index resource " + resource.address, e)
          sender ! ResourceIndexingFailed(resource.address)
        }).apply{
          log.trace("Got request to index {}", resource.address)

          if(shouldIndexResource(resource)){

            log.debug("Indexing resource {}", resource.address)

            sender ! ResourceIndexedMsg(resource.address, indexResource(resource))
            lastDocumentTime = System.currentTimeMillis
            if (writer.numDocs > maxDocsCount) {
              createNewWriter()
            }
          }else{
            log.debug("No need to index resource {}", resource.address)
          }
        }
      }

      case SetMaxDocsCountMsg(count) => maxDocsCount = count

      case FlushIndex(force) => {
        if (writer.numDocs > 0) {
          if (force)
            createNewWriter()
          else {
            //More than 30 seconds between resources
            if (System.currentTimeMillis - lastDocumentTime > 30 * 1000)
              createNewWriter()
          }
        }else{
          log trace num + ": Writer is empty, flush skipped"
        }
      }

      case UpdateTextExtractor(extractor) => textExtractor = extractor

      case DestroyMsg => {

        handling(classOf[Throwable]).by(
          e => log.warn(num + ": Failed to store indexer", e)
        ).andFinally{
          sender ! DestroyMsgReply
        }.apply{

        }
      }
    }


    override def postStop(){
      log.info(num + ": Stopping memory indexer")
      writer.close()
      fileIndexer.mergeIndex(directory)
    }

    def indexResource(resource: Resource): Map[String, String] = {
      log.debug("{}: Indexing resource {}", num,resource.address)

      val extractedDocument = if(extractDocumentContent){
        textExtractor.extract(resource)
      }else None

      try{
        val language = getLanguage(resource.metadata, extractedDocument)

        val document = new WeightedDocumentBuilder(configurationManager.searchConfiguration).build(resource,
          extractedDocument, language)

        captureDocumentFields(document)

        if(log.isDebugEnabled){
          log.debug("Lucene document: {}", document.toString)
        }

        writer.addDocument(document, analyzer(language))
        writer.commit()

        log.debug("Resource written to tmp index ({})", resource.address)

        extractedDocument match {
          case Some(doc) => doc.metadata
          case None => Map()
        }
      }finally{
        extractedDocument.map(_.dispose())
      }
    }

    def analyzer(lang: Option[String]):Analyzer = {
      lang match {
        case Some("ru") => new RussianAnalyzer(LUCENE_VERSION)
        case _ => new StandardAnalyzer(LUCENE_VERSION)
      }
    }

    def captureDocumentFields(document:Document){
      val indexedFieldList = asScalaBuffer(document.getFields).filter(_.isTokenized).map(_.name()).toList

      configurationManager ! HandleFieldListMsg(indexedFieldList)
    }

    def getLanguage(metadata: Metadata, extracted: Option[ExtractedDocument]): Option[String] = {

      (extracted match {
        case Some(document) => Some(document.content)
        case None => metadata(TITLE)
      }).flatMap(text => Some(languageGuesser.guessLanguage(new StringReader(text))))
    }

    def shouldIndexResource(resource:Resource):Boolean = {
      !resource.systemMetadata.asMap.contains(Constants.C3_MD_SKIP_IDX)
    }
  }
}

case class ResourceIndexingFailed(address: String)
case class ResourceIndexedMsg(address: String, extractedMetadata: Map[String, String])
case class IndexMsg(resource: Resource)
case class SetMaxDocsCountMsg(count: Int)
case class FlushIndex(force: Boolean)
case class UpdateTextExtractor(extractor: TextExtractor)
