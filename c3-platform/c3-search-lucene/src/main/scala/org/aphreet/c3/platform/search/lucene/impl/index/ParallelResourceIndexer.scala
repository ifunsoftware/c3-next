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
import java.util.concurrent.{TimeUnit, LinkedBlockingQueue, Executors}
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.apache.lucene.misc.LanguageGuesser
import org.aphreet.c3.platform.common.{Constants, Logger}
import org.aphreet.c3.platform.resource.{Metadata, Resource}
import org.aphreet.c3.platform.search.lucene.impl.SearchComponentProtocol.{ResourceIndexedMsg, ResourceIndexingFailed}
import org.aphreet.c3.platform.search.lucene.impl.SearchManagerInternal.LUCENE_VERSION
import org.aphreet.c3.platform.search.lucene.impl.common.Fields._
import org.aphreet.c3.platform.search.lucene.impl.common.{Fields, LanguageGuesserUtil}
import org.aphreet.c3.platform.search.lucene.impl.index.extractor.ExtractedDocument
import org.aphreet.c3.platform.search.lucene.{HandleFieldListMsg, SearchConfigurationManager}


class ParallelResourceIndexer(val threads: Int, val indexHolder: IndexHolder,
                              val configurationManager:SearchConfigurationManager,
                              var extractDocumentContent:Boolean,
                              var textExtractor:TextExtractor) extends ResourceIndexer {

  val log = Logger(getClass)

  private val executor = Executors.newFixedThreadPool(threads)

  private val guessers = new LinkedBlockingQueue[LanguageGuesser]()

  {
    for(number <- 1 to threads){
      guessers.put(LanguageGuesserUtil.createGuesser())
    }
  }

  def index(resource: Resource, sender: ActorRef){
    executor.submit(new Runnable {
      def run(){
        indexResource(resource, sender)
      }
    })
  }

  def delete(address: String){
    executor.submit(new Runnable{
      def run(){
        deleteResource(address)
      }
    })
  }

  private def indexResource(resource: Resource, sender: ActorRef) = {
    if(shouldIndexResource(resource)){
      log.debug("Indexing resource {}", resource.address)


      val extractedDocument = if(extractDocumentContent){
        textExtractor.extract(resource)
      }else None
      try{
        val language = getLanguage(resource.metadata, extractedDocument)

        val document = new WeightedDocumentBuilder(configurationManager.searchConfiguration).build(resource,
          extractedDocument, language)

        if(log.isDebugEnabled){
          log.debug("Lucene document: {}", document.toString)
        }

        deleteResource(resource.address)
        indexHolder.addDocument(document, analyzer(language))
        captureDocumentFields(document)
        sender ! ResourceIndexedMsg(resource.address, extractedDocument match {
          case Some(doc) => doc.metadata
          case None => Map()
        })

        log.debug("Resource indexed {}", resource.address)
      }catch{
        case e: Throwable => {
          log.warn("Failed to index document " + resource.address, e)
          sender ! ResourceIndexingFailed(resource.address)
        }
      }finally{
        extractedDocument.map(_.dispose())
      }
    }
  }

  def deleteResource(address:String) = {
    indexHolder.deleteDocuments(new Term(Fields.ADDRESS, address))
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

    val languageGuesser = guessers.take()

    try{
      (extracted match {
        case Some(document) => Some(document.content)
        case None => metadata(TITLE)
      }).flatMap(text => Some(languageGuesser.guessLanguage(new StringReader(text))))
    }finally{
      guessers.put(languageGuesser)
    }
  }

  def shouldIndexResource(resource:Resource):Boolean = {
    !resource.systemMetadata.asMap.contains(Constants.C3_MD_SKIP_IDX)
  }

  def updateTextExtractor(textExtractor: TextExtractor){
    this.textExtractor = textExtractor
  }

  def setDocumentExtractionRequired(flag: Boolean){
    this.extractDocumentContent = flag
  }

  def destroy(){
    executor.shutdown()
    executor.awaitTermination(10, TimeUnit.MINUTES)
    guessers.clear()
  }
}

