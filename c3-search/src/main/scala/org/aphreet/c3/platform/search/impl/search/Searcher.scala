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

import org.apache.commons.logging.LogFactory
import actors.Actor
import actors.Actor._
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.aphreet.c3.platform.common.{WatchedActor, Path}
import org.aphreet.c3.platform.search.SearchResultElement
import org.aphreet.c3.search.ext.{SearchConfiguration, SearchStrategyFactory}
import org.aphreet.c3.platform.search.impl.index.RamIndexer
import org.apache.lucene.search._
import org.apache.lucene.search.{Searcher => LuceneSearcher}


class Searcher(var indexPath: Path, var ramIndexers:List[RamIndexer], val configuration:SearchConfiguration) extends WatchedActor{

  var searchStrategyFactory:SearchStrategyFactory = _

  {
    searchStrategyFactory = new SearchStrategyFactory(configuration)
  }


  val log = LogFactory.getLog(getClass)

  var indexSearcher = createSearcher


  def act{
    loop{
      react{
        case ReopenSearcher => {
          log info "Reopening searcher"
          val oldSearcher = indexSearcher

          indexSearcher = createSearcher

          Thread.sleep(1000 * 5) //May be some threads is still using old searcher

          oldSearcher.close
        }

        case NewIndexPathMsg(path) => {
          log info "Changing index path to " + path
          indexPath = path
          this ! ReopenSearcher
        }

        case DestroyMsg => {
          log info "Destroying searcher"
          try{
            indexSearcher.close
          }finally{
            this.exit
          }

        }
      }
    }
  }

  private def createSearcher:LuceneSearcher = {
    new ParallelMultiSearcher(
    (new IndexSearcher(indexPath.file.getCanonicalPath)
      :: ramIndexers.map(indexer => new IndexSearcher(indexer.directory)).toList).toArray)
  }

  def getSearcher:LuceneSearcher = indexSearcher

  def search(domain:String, sourceQuery: String): Array[SearchResultElement] = {

    val searchStrategy = searchStrategyFactory.createSearchStrategy;

    val found = searchStrategy.search(getSearcher, sourceQuery, 30, 0, domain)

    val results = new Array[SearchResultElement](found.size)

    var i = 0

    for(e <- found){

      results(i) = SearchResultElement.fromEntry(e)

      i = i +1
    }

    if(log.isDebugEnabled)
      log debug "results: " + results.toString

    results
  }


  def close {
    this ! DestroyMsg
  }

}

object ReopenSearcher

case class NewIndexPathMsg(path:Path)
