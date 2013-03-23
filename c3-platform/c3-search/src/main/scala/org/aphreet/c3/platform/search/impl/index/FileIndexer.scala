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

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.store.{NIOFSDirectory, Directory}
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.aphreet.c3.platform.search.impl.search._
import org.apache.lucene.index.{IndexWriterConfig, Term, IndexWriter}
import org.aphreet.c3.platform.search.impl.common.Fields
import org.aphreet.c3.platform.common.{Logger, WatchedActor, Path}
import org.apache.lucene.util.Version
import org.aphreet.c3.platform.search.impl.search.NewIndexPathMsg

class FileIndexer(var indexPath:Path) extends WatchedActor{

  val log = Logger(getClass)

  var searcher:Searcher = null

  var indexWriter:IndexWriter = createWriter(indexPath)

  private def createWriter(path:Path):IndexWriter = {
    log info "Creating IndexWriter"

    val directory = new NIOFSDirectory(path.file)

    if(IndexWriter.isLocked(directory)){
      log warn "Index path is locked, unlocking..."
      IndexWriter.unlock(directory)
    }

    new IndexWriter(directory,
      new IndexWriterConfig(Version.LUCENE_35,
        new StandardAnalyzer(Version.LUCENE_35))
          .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))
  }

  def act(){
    loop{
      react{
        case MergeIndexMsg(directory) =>
          try{

            indexWriter.addIndexes(directory)
            indexWriter.commit()
            directory.close()
            log debug "Index merged"
            searcher ! ReopenSearcherMsg
          }catch{
            case e: Throwable =>
              log.warn("Failed to merge index", e)
          }
        case DeleteMsg(address) => deleteResource(address)

        case DeleteIndexMsg => {
          try{
            log.info("Deleting search index")
            indexWriter.deleteAll()
            indexWriter.commit()
            searcher ! ReopenSearcherMsg
          }catch{
            case e : Throwable => "Failed to delete search index"
          }
        }

        case DeleteForUpdateMsg(resource) =>
          deleteResource(resource.address)
          sender ! IndexMsg(resource)

        case NewIndexPathMsg(path) => {
          try{
            log info "Changing index path to " + path
            indexPath = path
            indexWriter.close()
            indexWriter = createWriter(indexPath)
            log info "Index path changed"
            searcher ! NewIndexPathMsg(path)
            sender ! UpdateIndexCreationTimestamp(System.currentTimeMillis + 5000) //5 seconds offset
          }catch{
            case e: Throwable => "Failed to create new indexWriter"
          }
        }

        case DestroyMsg => {
          try{
            indexWriter.close()
          }catch{
            case e: Throwable => log.warn("Failed to close index", e)
            throw e
          }finally {
            log info "IndexWriter closed"
            this.exit()
          }
        }
      }
    }
  }

  def deleteResource(address:String) {
    try{
      val term = new Term(Fields.ADDRESS, address)
      indexWriter.deleteDocuments(term)
      log debug "Documents with term address:" + address + " have been deleted"
    }catch{
      case e: Throwable => log.error("Failed to delete resource, e is: ", e)
    }
  }
}

case class MergeIndexMsg(directory:Directory)
object DeleteIndexMsg
case class DeleteMsg(address:String)
case class DeleteForUpdateMsg(resource:Resource)
case class UpdateIndexCreationTimestamp(time:Long)
