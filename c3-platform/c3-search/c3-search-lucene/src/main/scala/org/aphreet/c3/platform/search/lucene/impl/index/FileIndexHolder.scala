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
import org.aphreet.c3.platform.search.lucene.impl.common.{LanguageGuesserUtil, Fields}
import org.aphreet.c3.platform.search.lucene.impl.search._
import akka.actor.ActorRef
import org.apache.lucene.document.Document
import org.apache.lucene.analysis.Analyzer
import scala.concurrent.Future
import java.util.concurrent.locks.{ReentrantReadWriteLock, Lock}
import java.util.concurrent.atomic.AtomicLong
import org.aphreet.c3.platform.search.lucene.impl.SearchComponentProtocol.UpdateIndexCreationTimestamp


class FileIndexHolder(var indexPath:Path, val searcher: Searcher) extends IndexHolder{

  private val executor = Executors.newSingleThreadExecutor()

  private val log = Logger(getClass)

  private var indexWriter = createWriter(indexPath)

  private val lock = new ReentrantReadWriteLock()

  private val modificationCount = new AtomicLong()

  private val indexScheduler = new SearchIndexScheduler()
  indexScheduler.start()

  private def inReadLock(block: => Any){
    val readLock = lock.readLock()
    try{
      readLock.lock()
      block
    }finally {
      readLock.unlock()
    }
  }

  private def inWriteLock(block: => Any){
    val writeLock = lock.writeLock()
    try{
      writeLock.lock()
      block
    }finally {
      writeLock.unlock()
    }
  }

  def addDocument(document: Document, analyzer: Analyzer) = {
    inReadLock{
      indexWriter.addDocument(document, analyzer)
      indexWriter.commit()
      modificationCount.incrementAndGet()
    }
  }

  def deleteDocuments(term: Term) = {
    inReadLock{
      indexWriter.deleteDocuments(term)
      indexWriter.commit()
      modificationCount.incrementAndGet()
    }
  }


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

  def deleteIndex(caller: ActorRef){

    log.info("Submitting task to delete search index")
    executor.submit(new Runnable {
      def run(){
        inWriteLock{
          try{
            log.info("Deleting search index")
            indexWriter.deleteAll()
            indexWriter.commit()
            modificationCount.set(0)
            searcher ! ReopenSearcherMsg
            caller ! UpdateIndexCreationTimestamp(System.currentTimeMillis())
          }catch{
            case e : Throwable => log.warn("Failed to delete search index", e)
          }
        }
      }
    })
  }

  def destroy() {

    indexScheduler.interrupt()

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
        inWriteLock{
          try{

            log info "Changing index path to " + path
            indexPath = path
            indexWriter.close()
            indexWriter.getDirectory.close()

            indexWriter = createWriter(indexPath)
            log info "Index path changed"
            modificationCount.set(0)
            searcher ! NewIndexPathMsg(path)
            caller ! UpdateIndexCreationTimestamp(System.currentTimeMillis + 5000) //5 seconds offset
          }catch{
            case e: Throwable => log.warn("Failed to create new indexWriter", e)
          }
        }
      }
    })
  }

  def reopenSearcher(){
    if(modificationCount.get() > 0){
      searcher ! ReopenSearcherMsg
      modificationCount.set(0)
    }
  }

  class SearchIndexScheduler() extends Thread{

    val log = Logger(getClass)

    {
      this.setDaemon(true)
    }

    override def run(){

      log info "Started scheduler"

      while(!Thread.currentThread.isInterrupted){
        try{
          Thread.sleep(1000 * 60)
        }catch{
          case e:InterruptedException =>
            log info "Thread interrupted"
            Thread.currentThread.interrupt()
        }
        reopenSearcher()
      }

      log info "Search scheduler stopped"
    }
  }

}

