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
import actors.Actor._
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.store.{RAMDirectory, Directory}
import org.aphreet.c3.platform.resource.Resource
import org.apache.commons.logging.LogFactory
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.aphreet.c3.platform.common.msg.{DestroyMsgReply, DestroyMsg}
import java.io.StringReader
import org.aphreet.c3.platform.search.impl.common.Fields._
import org.aphreet.c3.platform.search.impl.common.LanguageGuesserUtil
import org.aphreet.c3.platform.common.WatchedActor
import org.aphreet.c3.search.ext.{DocumentBuilderFactory, SearchConfiguration}

class RamIndexer(val fileIndexer: FileIndexer,
                 val configuration:SearchConfiguration, num: Int,
                 var extractDocumentContent:Boolean) extends WatchedActor {

  val log = LogFactory.getLog(getClass)

  var maxDocsCount: Int = 100

  var directory: Directory = null

  var writer: IndexWriter = null

  var lastDocumentTime: Long = System.currentTimeMillis

  val textExtractor = new TextExtractor

  val languageGuesser = LanguageGuesserUtil.createGuesser

  var documentBuilderFactory:DocumentBuilderFactory = _

  {
    createNewWriter()

    documentBuilderFactory = new DocumentBuilderFactory(configuration)
  }


  def createNewWriter() {

    val oldWriter = writer
    val oldDirectory = directory

    directory = new RAMDirectory

    writer = new IndexWriter(directory, new StandardAnalyzer, IndexWriter.MaxFieldLength.UNLIMITED)

    if (oldWriter != null) {
      oldWriter.close()
      fileIndexer ! MergeIndexMsg(oldDirectory)
    }
  }


  def act() {
    loop {
      react {
        case IndexMsg(resource) => {
          try {

            if(shouldIndexResource(resource)){

              indexResource(resource)
              sender ! ResourceIndexedMsg(resource.address)
              lastDocumentTime = System.currentTimeMillis
              if (writer.numDocs > maxDocsCount) {
                createNewWriter()
              }
            }
          } catch {
            case e => log.warn(num + ": Failed to index resource", e)
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
          try {
            log info num + ": Stopping memory indexer"
            writer.close()
            fileIndexer ! MergeIndexMsg(directory)

          } catch {
            case e => log.warn(num + ": Failed to store indexer", e)
            throw e
          } finally {
            reply {
              DestroyMsgReply
            }
            this.exit()
          }
        }
      }
    }
  }

  def indexResource(resource: Resource) {
    log debug num + ": Indexing resource " + resource.address

    val extractedMeta =
      if(extractDocumentContent){
        textExtractor.extract(resource)
      }else{
        Map[String, String]()
      }

    val metadata = Map[String, String]() ++ resource.metadata
    val language = getLanguage(metadata, extractedMeta)


    val resourceHandler = new ResourceHandler(documentBuilderFactory, resource, metadata, extractedMeta, language)

    val document = resourceHandler.document
    val analyzer = resourceHandler.analyzer

    writer.addDocument(document, analyzer)
    log debug "Resource writen to tmp index (" + resource.address + ")"
  }

  def getLanguage(metadata:Map[String, String], extracted:Map[String, String]):String = {
    var str:String = null

    if(extracted.contains(CONTENT))
      str = extracted.get(CONTENT).get
    else if(metadata.contains(TITLE)){
      str = metadata.get(TITLE).get
    }

    if(str != null){
      languageGuesser.guessLanguage(new StringReader(str))
    }else{
      null
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