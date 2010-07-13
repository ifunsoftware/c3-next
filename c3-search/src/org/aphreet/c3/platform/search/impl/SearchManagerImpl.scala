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
package org.aphreet.c3.platform.search.impl

import actors.Actor._

import index._
import org.aphreet.c3.platform.search.SearchManager
import org.aphreet.c3.platform.access._
import org.springframework.stereotype.Component
import javax.annotation.{PreDestroy, PostConstruct}
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.common.msg._
import org.aphreet.c3.platform.common.Path
import java.util.Date
import org.aphreet.c3.platform.management.{PropertyChangeEvent, SPlatformPropertyListener}

@Component("searchManager")
class SearchManagerImpl extends SearchManager with SPlatformPropertyListener {
  val INDEX_PATH = "c3.search.index.path"
  val INDEXER_COUNT = "c3.search.index.count"
  val MAX_TMP_INDEX_SIZE = "c3.search.index.max_size"

  val log = LogFactory.getLog(getClass)

  var accessManager: AccessManager = _

  var fileIndexer: FileIndexer = _

  var indexPath: Path = new Path("/path/to/file")

  var ramIndexers: List[RamIndexer] = List()

  val random = new java.util.Random(System.currentTimeMillis)

  @Autowired
  def setAccessManager(manager: AccessManager) = {accessManager = manager}

  @PostConstruct
  def init {
    log info "Starting SearchManager"

    if (indexPath != null) {
      fileIndexer = new FileIndexer(indexPath)

      ramIndexers = new RamIndexer(fileIndexer) :: ramIndexers

      this.start
      accessManager ! RegisterListenerMsg(this)
    } else {
      log warn "Index path is not set. Waiting for property to appear"
    }
  }

  @PreDestroy
  def destroy {
    log info "Destroying SearchManager"
    accessManager ! UnregisterListenerMsg(this)
    this ! DestroyMsg

    ramIndexers.foreach(_ ! DestroyMsg)
    fileIndexer ! DestroyMsg
    
  }

  def search(query: String): List[String] = List()

  def act {
    while (true) {
      receive {
        case ResourceAddedMsg(resource) => selectIndexer ! IndexMsg(resource)

        case ResourceUpdatedMsg(resource) => fileIndexer ! DeleteForUpdateMsg(resource)

        case ResourceDeletedMsg(address) => fileIndexer ! DeleteMsg(address)

        case IndexMsg(resource) => selectIndexer ! IndexMsg(resource)

        case ResourceIndexedMsg(address) =>
          accessManager ! UpdateMetadataMsg(address, Map("indexed" -> new Date().getTime.toString))
        case DestroyMsg =>
          log info "Destroying SearchManager actor"
          this.exit
      }
    }
  }

  def selectIndexer: RamIndexer = {
    val num = Math.abs(random.nextInt) % (ramIndexers.size)
    ramIndexers.drop(num).first
  }

  def defaultValues: Map[String, String] = Map(
    INDEXER_COUNT -> "2",
    MAX_TMP_INDEX_SIZE -> "100"
    )

  def listeningForProperties: Array[String] = Array(
    INDEX_PATH, INDEXER_COUNT, MAX_TMP_INDEX_SIZE
    )

  def propertyChanged(event: PropertyChangeEvent) {
    event.name match {
      case INDEX_PATH =>

      case INDEXER_COUNT => {
        val newCount = Integer.parseInt(event.newValue)

        if(ramIndexers.size < newCount){
          for(i <- 1 to newCount - ramIndexers.size){
            val indexer = new RamIndexer(fileIndexer)
            indexer.start
            ramIndexers = indexer :: ramIndexers
          }
        }else if(ramIndexers.size > newCount){
          val dropCount = ramIndexers.size - newCount
          val toStop = ramIndexers.take(dropCount)
          ramIndexers = ramIndexers.drop(dropCount)

          toStop.foreach(_ ! DestroyMsg)
        }
      }

      case MAX_TMP_INDEX_SIZE =>
        if(event.newValue != event.oldValue)
          ramIndexers.foreach(_ ! SetMaxDocsCountMsg(Integer.parseInt(event.newValue)))
      case _ =>
    }
  }

}