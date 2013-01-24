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

package org.aphreet.c3.platform.search.impl.index

import actors.Actor
import org.apache.lucene.index.{IndexWriterConfig, IndexWriter}
import org.apache.lucene.store.{RAMDirectory, Directory}
import org.aphreet.c3.platform.resource.Resource
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.aphreet.c3.platform.common.msg.{DestroyMsgReply, DestroyMsg}
import java.io.{Reader, StringReader}
import org.aphreet.c3.platform.search.impl.common.Fields._
import org.aphreet.c3.platform.search.impl.common.LanguageGuesserUtil
import org.aphreet.c3.platform.common.{Tracer, WatchedActor}
import org.aphreet.c3.platform.search.ext.DocumentBuilderFactory
import org.apache.lucene.util.Version
import collection.JavaConversions._
import org.apache.lucene.document.Document
import org.aphreet.c3.platform.search.{HandleFieldListMsg, SearchConfigurationManager}
import scala.util.control.Exception._
import org.aphreet.c3.platform.search.impl.index.extractor.ExtractedDocument


class RamIndexer(val fileIndexer: Actor,
                 val configurationManager:SearchConfigurationManager, num: Int,
                 var extractDocumentContent:Boolean,
                 val textExtractor:TextExtractor) extends WatchedActor with Tracer {

  val log = logOfClass(getClass)

  var maxDocsCount: Int = 100

  var directory: Directory = null

  var writer: IndexWriter = null

  var lastDocumentTime: Long = System.currentTimeMillis

  val languageGuesser = LanguageGuesserUtil.createGuesser

  var documentBuilderFactory:DocumentBuilderFactory = _

  {
    createNewWriter()

    documentBuilderFactory = new DocumentBuilderFactory()
  }


  def createNewWriter() {

    val oldWriter = writer
    val oldDirectory = directory

    directory = new RAMDirectory

    writer = new IndexWriter(directory, new IndexWriterConfig(Version.LUCENE_35, new StandardAnalyzer(Version.LUCENE_35)))

    if (oldWriter != null) {
      oldWriter.close()
      fileIndexer ! MergeIndexMsg(oldDirectory)
    }
  }


  def act() {
    loop {
      react {
        case IndexMsg(resource) => {
          handling(classOf[Throwable]).by(e => log.warn(num + ": Failed to index resource", e)).apply{
            trace{"Got request to index " + resource.address}

            if(shouldIndexResource(resource)){

              debug{"Indexing resource " + resource.address}

              indexResource(resource)
              sender ! ResourceIndexedMsg(resource.address)
              lastDocumentTime = System.currentTimeMillis
              if (writer.numDocs > maxDocsCount) {
                createNewWriter()
              }
            }else{
              debug{"No need to index resource " + resource.address}
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

        case DestroyMsg => {

          handling(classOf[Throwable]).by(
            e => log.warn(num + ": Failed to store indexer", e)
          ).andFinally{
            reply { DestroyMsgReply}
            this.exit()
          }.apply{
            log.info(num + ": Stopping memory indexer")
            writer.close()
            fileIndexer ! MergeIndexMsg(directory)
          }
        }
      }
    }
  }

  def indexResource(resource: Resource) {
    debug{ num + ": Indexing resource " + resource.address}

    val extractedDocument = if(extractDocumentContent){
      textExtractor.extract(resource)
    }else None

    try{
      val metadata = Map[String, String]() ++ resource.metadata
      val language = getLanguage(metadata, extractedDocument)

      val resourceHandler = new ResourceHandler(documentBuilderFactory,
        configurationManager.searchConfiguration, resource, metadata, extractedDocument, language)

      val document = resourceHandler.document
      val analyzer = resourceHandler.analyzer

      captureDocumentFields(document)

      debug{"Lucene document: " + document.toString}

      writer.addDocument(document, analyzer)
      writer.commit()
    }finally{
      extractedDocument match {
        case Some(document) => document.dispose()
        case None =>
      }
    }

    debug{ "Resource writen to tmp index (" + resource.address + ")"}
  }

  def captureDocumentFields(document:Document){
    val indexedFieldList = asScalaBuffer(document.getFields).filter(_.isTokenized).map(_.name()).toList

    configurationManager ! HandleFieldListMsg(indexedFieldList)
  }

  def getLanguage(metadata:Map[String, String], extracted: Option[ExtractedDocument]):String = {

    val reader: Option[Reader] = extracted match {
      case Some(document) => Some(new StringReader(document.content))
      case None => metadata.get(TITLE) match {
        case Some(value) => Some(new StringReader(value))
        case None => None
      }
    }

    reader match {
      case Some(value) => languageGuesser.guessLanguage(value)
      case None => null
    }
  }

  def shouldIndexResource(resource:Resource):Boolean = {
    resource.systemMetadata.get("c3.skip.index") match{
      case Some(x) => false
      case None => true
    }
  }
}

case class ResourceIndexedMsg(address: String)
case class IndexMsg(resource: Resource)
case class SetMaxDocsCountMsg(count: Int)
case class FlushIndex(force: Boolean)
