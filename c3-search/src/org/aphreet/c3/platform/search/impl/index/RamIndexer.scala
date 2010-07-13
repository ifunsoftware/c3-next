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
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.store.{RAMDirectory, Directory}
import org.aphreet.c3.platform.resource.Resource
import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.apache.lucene.analysis.standard.StandardAnalyzer

class RamIndexer(val fileIndexer: FileIndexer) extends Actor {
  val log = LogFactory.getLog(getClass)

  var maxDocsCount: Int = 100

  var directory: Directory = null

  var writer: IndexWriter = null

  {
    createNewWriter
  }

  def createNewWriter = {
    if (writer != null) {
      writer.close
      fileIndexer ! MergeIndexMsg(directory)
    }

    directory = new RAMDirectory

    writer = new IndexWriter(directory, new StandardAnalyzer)
  }


  def act() {
    while (true) {
      receive {
        case IndexMsg(resource) => {
          try {
            indexResource(resource)
            sender ! ResourceIndexedMsg(resource.address)
            if (writer.numDocs > 100) {
              createNewWriter
            }
          } catch {
            case e => log.warn("Failed to index resource", e)
          }
        }

        case SetMaxDocsCountMsg(count) => maxDocsCount = count

        case DestroyMsg => {
          try {
            log info "Stopping memory indexer"
            this.exit
            writer.close()
            fileIndexer ! MergeIndexMsg(directory)
          } catch {
            case e => log.warn("Failed to store indexer", e)
          }

        }

      }
    }
  }

  def indexResource(resource: Resource) = {
    //TODO implement this magic!
  }
}

case class ResourceIndexedMsg(address: String)
case class IndexMsg(resource: Resource)
case class SetMaxDocsCountMsg(count: Int)