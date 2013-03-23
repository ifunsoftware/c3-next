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

import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.aphreet.c3.platform.common.{Logger, WatchedActor, Path}
import org.aphreet.c3.platform.search.{SearchConfigurationManager, SearchResultElement}
import org.aphreet.c3.platform.search.impl.index.RamIndexer
import org.apache.lucene.search._
import org.apache.lucene.index.IndexReader
import org.apache.lucene.store.NIOFSDirectory


class Searcher(var indexPath: Path,
               var ramIndexers: List[RamIndexer],
               val configurationManager: SearchConfigurationManager) extends WatchedActor {


  val log = Logger(getClass)

  var indexSearcher = createSearcher


  def act() {
    loop {
      react {
        case ReopenSearcherMsg => {
          log info "Reopening searcher"
          val oldSearcher = indexSearcher

          indexSearcher = createSearcher

          Thread.sleep(1000 * 5) //May be some threads are still using old searcher

          oldSearcher match {
            case Some(value) => value.close()
            case None =>
          }
        }

        case NewIndexPathMsg(path) => {
          log info "Changing index path to " + path
          indexPath = path
          this ! ReopenSearcherMsg
        }

        case DestroyMsg => {
          log info "Destroying searcher"
          try {
            indexSearcher match {
              case Some(value) => value.close()
              case None =>
            }
          } finally {
            this.exit()
          }

        }
      }
    }
  }

  private def createSearcher: Option[IndexSearcher] = {

    try {
      val reader = IndexReader.open(new NIOFSDirectory(indexPath.file.getCanonicalFile))

      Some(new IndexSearcher(reader))
    } catch {
      case e: Throwable => {
        log.warn("Failed to open IndexSearcher due to exception: " + e.getMessage)
        None
      }
    }

    //TODO Add temp directories to the result in the future
    //(reader :: ramIndexers.map(indexer => IndexReader.open(indexer.directory)).toList).toArray
  }

  def getSearcher: Option[IndexSearcher] = indexSearcher

  def search(domain: String, sourceQuery: String): Array[SearchResultElement] = {

    if (getSearcher.isEmpty) {
      Array()
    } else {
      new MultiFieldSearchStrategy().search(getSearcher.get,
        configurationManager.searchConfiguration,
        sourceQuery, 30, 0, domain)
    }
  }


  def close() {
    this ! DestroyMsg
  }

}

object ReopenSearcherMsg

case class NewIndexPathMsg(path: Path)
