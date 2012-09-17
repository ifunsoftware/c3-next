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
import org.apache.commons.logging.LogFactory
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.store.{Directory, FSDirectory}
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.aphreet.c3.platform.search.impl.search.{ReopenSearcher, Searcher}
import org.apache.lucene.index.{Term, IndexWriter}
import org.aphreet.c3.platform.search.impl.common.Fields
import org.aphreet.c3.platform.search.impl.search.NewIndexPathMsg
import org.aphreet.c3.platform.common.{WatchedActor, Path}

class FileIndexer(var indexPath:Path) extends WatchedActor{

  val log = LogFactory.getLog(getClass)

  var searcher:Searcher = null

  var indexWriter:IndexWriter = createWriter(indexPath)

  private def createWriter(path:Path):IndexWriter = {
    log info "Creating IndexWriter"

    val directory = FSDirectory.getDirectory(path.file)
    if(IndexWriter.isLocked(directory)){
      log warn "Index path is locked, unlocking..."
      IndexWriter.unlock(directory)
    }

    new IndexWriter(directory, new StandardAnalyzer, IndexWriter.MaxFieldLength.UNLIMITED)
  }

  def act(){
    loop{
      react{
        case MergeIndexMsg(directory) =>
          try{
            indexWriter.addIndexesNoOptimize(Array(directory))
            indexWriter.commit()
            indexWriter.optimize()
            log debug "Index merged"
            searcher ! ReopenSearcher
          }catch{
            case e =>
              log.warn("Failed to merge index", e)
          }
        case DeleteMsg(address) => deleteResource(address)

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
            case e => "Failed to create new indexWriter"
          }
        }

        case DestroyMsg => {
          try{
            indexWriter.close()
          }catch{
            case e => log.warn("Failed to close index", e)
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
      case e => log.error("Failed to delete resource, e is: ", e)
    }
  }
}

case class MergeIndexMsg(directory:Directory)
case class DeleteMsg(address:String)
case class DeleteForUpdateMsg(resource:Resource)
case class UpdateIndexCreationTimestamp(time:Long)