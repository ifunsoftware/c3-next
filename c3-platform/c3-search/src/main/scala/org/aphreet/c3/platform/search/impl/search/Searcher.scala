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
package org.aphreet.c3.platform.search.impl.search

import akka.actor.{Props, ActorRefFactory, Actor}
import org.apache.lucene.index.IndexReader
import org.apache.lucene.search._
import org.apache.lucene.store.NIOFSDirectory
import org.aphreet.c3.platform.common.{ActorRefHolder, Logger, Path}
import org.aphreet.c3.platform.search.{SearchResult, SearchConfigurationManager}


class Searcher(val actorSystem: ActorRefFactory, configurationManager: SearchConfigurationManager) extends ActorRefHolder {

  val log = Logger(getClass)

  var indexPath: Path = null

  var indexSearcher: Option[IndexSearcher] = None

  val async = actorSystem.actorOf(Props[SearcherActor])

  class SearcherActor extends Actor {

    def receive = {
      case ReopenSearcherMsg => {
        log info "Reopening searcher"
        val oldSearcher = indexSearcher

        try{
          indexSearcher = createSearcher(indexPath)

          Thread.sleep(1000 * 5) //May be some threads are still using old searcher

        }finally{
          closeSearcher(oldSearcher)
        }
      }

      case NewIndexPathMsg(path) => {
        log info "Changing index path to " + path
        indexPath = path
        self ! ReopenSearcherMsg
      }
    }

    override def postStop(){
      log.info("Destroying searcher")
      closeSearcher(indexSearcher)
    }

    private def closeSearcher(searcherOption: Option[IndexSearcher]){

      try{
        searcherOption.map(searcher => {
          searcher.close()
          searcher.getIndexReader.close()
          searcher.getIndexReader.directory().close()
        })
      }catch{
        case e: Throwable => log.warn("Failed to close searcher", e)
      }
    }

    private def createSearcher(indexPath: Path): Option[IndexSearcher] = {

      if(indexPath == null){
        log.warn("Index path is null")
        None
      }else{
        var reader: IndexReader = null

        try {
          reader = IndexReader.open(new NIOFSDirectory(indexPath.file.getCanonicalFile))

          Some(new IndexSearcher(reader))
        } catch {
          case e: Throwable => {
            log.warn("Failed to open IndexSearcher due to exception: " + e.getMessage)

            if(reader != null){
              reader.close()
            }

            None
          }
        }
      }

      //TODO Add temp directories to the result in the future
      //(reader :: ramIndexers.map(indexer => IndexReader.open(indexer.directory)).toList).toArray
    }
  }

  def search(domain: String, sourceQuery: String): SearchResult = {

    indexSearcher match {
      case Some(searcher) => new MultiFieldSearchStrategy().search(searcher,
        configurationManager.searchConfiguration,
        sourceQuery, 30, 0, domain)
      case None => SearchResult(sourceQuery, Array())
    }
  }

}

object ReopenSearcherMsg

case class NewIndexPathMsg(path: Path)
