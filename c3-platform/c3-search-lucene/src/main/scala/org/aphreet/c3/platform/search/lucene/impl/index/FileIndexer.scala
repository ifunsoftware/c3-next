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

import java.util.concurrent.{TimeUnit, Executors}
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.{IndexReader, IndexWriterConfig, Term, IndexWriter}
import org.apache.lucene.store.{NIOFSDirectory, Directory}
import org.aphreet.c3.platform.common.{Logger, Path}
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.search.lucene.impl.SearchManagerInternal.LUCENE_VERSION
import org.aphreet.c3.platform.search.lucene.impl.common.Fields
import org.aphreet.c3.platform.search.lucene.impl.search._
import akka.actor.ActorRef


class FileIndexer(var indexPath:Path, val searcher: Searcher) {

  private val executor = Executors.newSingleThreadExecutor()

  private val log = Logger(getClass)

  private var indexWriter = createWriter(indexPath)

  private def createWriter(path:Path):IndexWriter = {
    log info "Creating IndexWriter"

    val directory = new NIOFSDirectory(path.file)

    if(IndexWriter.isLocked(directory)){
      log warn "Index path is locked, unlocking..."
      IndexWriter.unlock(directory)
    }

    new IndexWriter(directory,
      new IndexWriterConfig(LUCENE_VERSION,
        new StandardAnalyzer(LUCENE_VERSION))
        .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))
  }

  def mergeIndex(directory: Directory){
    executor.submit(new IndexMerger(directory))
  }

  def deleteIndex(){
    log.info("Submitting task to delete search index")
    executor.submit(new Runnable {
      def run(){
        try{
          log.info("Deleting search index")
          indexWriter.deleteAll()
          indexWriter.commit()
          searcher ! ReopenSearcherMsg
        }catch{
          case e : Throwable => log.warn("Failed to delete search index", e)
        }
      }
    })
  }

  class IndexMerger(val directory: Directory) extends Runnable {
    def run() {
      try{
        val reader = IndexReader.open(directory)

        //Make sure we don't have duplicates in index
        for (docIndex <- 0 until reader.numDocs()){
          val document = reader.document(docIndex)
          indexWriter.deleteDocuments(new Term(Fields.ADDRESS, document.get(Fields.ADDRESS)))
        }


        indexWriter.addIndexes(reader)
        indexWriter.commit()

        reader.close()
        directory.close()

        log debug "Index merged"
        searcher ! ReopenSearcherMsg
      }catch{
        case e: Throwable =>
          log.warn("Failed to merge index", e)
      }
    }
  }

  def destroy() {

    log.info("Stopping FileIndexer")
    executor.shutdown()
    executor.awaitTermination(10, TimeUnit.MINUTES)
    log.info("Closing file index")

    indexWriter.close()
    indexWriter.getDirectory.close()

    log.info("FileIndexer stopped")
  }

  def updateIndexLocation(path: Path, caller: ActorRef){

    executor.submit(new Runnable {
      def run(){
        try{
          log info "Changing index path to " + path
          indexPath = path
          indexWriter.close()
          indexWriter.getDirectory.close()

          indexWriter = createWriter(indexPath)
          log info "Index path changed"
          searcher ! NewIndexPathMsg(path)
          caller ! UpdateIndexCreationTimestamp(System.currentTimeMillis + 5000) //5 seconds offset
        }catch{
          case e: Throwable => log.warn("Failed to create new indexWriter", e)
        }
      }
    })
  }

  def deleteForIndex(resource: Resource, caller: ActorRef){
    executor.submit(new Runnable {
      def run(){
        deleteResource(resource.address)
        caller ! IndexMsg(resource)
      }
    })
  }

  def delete(address: String){
    executor.submit(new Runnable {
      def run(){
        deleteResource(address)
      }
    })
  }

  private def deleteResource(address:String) {
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
