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

import background.BackgroundIndexTask
import index._
import org.aphreet.c3.platform.access._
import org.springframework.stereotype.Component
import javax.annotation.{PreDestroy, PostConstruct}
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.common.msg._
import org.aphreet.c3.platform.common.Path
import java.util.Date
import org.aphreet.c3.platform.management.{PropertyChangeEvent, SPlatformPropertyListener}
import org.aphreet.c3.platform.config.{UnregisterMsg, RegisterMsg, PlatformConfigManager}
import org.aphreet.c3.platform.search.{SearchResultEntry, SearchManager}
import search.Searcher
import org.aphreet.c3.platform.task.TaskManager
import org.aphreet.c3.platform.storage.StorageManager
import org.aphreet.c3.platform.statistics.{IncreaseStatisticsMsg, StatisticsManager}
import org.aphreet.c3.platform.resource.Resource

@Component("searchManager")
class SearchManagerImpl extends SearchManager with SPlatformPropertyListener {

  val INDEX_PATH = "c3.search.index.path"

  val INDEXER_COUNT = "c3.search.index.count"

  val MAX_TMP_INDEX_SIZE = "c3.search.index.max_size"


  val log = LogFactory.getLog(getClass)


  var accessManager: AccessManager = _

  var configManager: PlatformConfigManager = _

  var taskManager: TaskManager = _

  var storageManager: StorageManager = _

  var statisticsManager: StatisticsManager = _


  var fileIndexer: FileIndexer = null

  var indexPath: Path = null

  var ramIndexers: List[RamIndexer] = List()

  val random = new java.util.Random(System.currentTimeMillis)

  var searcher:Searcher = null

  val indexScheduler = new SearchIndexScheduler(this)

  var indexerTaskId:String = null

  @Autowired
  def setAccessManager(manager: AccessManager) = {accessManager = manager}

  @Autowired
  def setConfigManager(manager: PlatformConfigManager) = {configManager = manager}

  @Autowired
  def setTaskManager(manager:TaskManager) = {taskManager = manager}

  @Autowired
  def setStorageManager(manager:StorageManager) = {storageManager = manager}

  @Autowired
  def setStatisticsManager(manager:StatisticsManager) = {statisticsManager = manager}

  @PostConstruct
  def init {

    configManager ! RegisterMsg(this)

    if (indexPath != null) {
      initialize
    } else {
      log warn "Index path is not set. Waiting for property to appear"
    }
  }

  def initialize() {
    if (fileIndexer == null) {

      fileIndexer = new FileIndexer(indexPath)

      searcher = new Searcher(indexPath)
      fileIndexer.searcher = searcher
      searcher.start

      fileIndexer.start

      ramIndexers = new RamIndexer(fileIndexer, 1) :: ramIndexers
      ramIndexers = new RamIndexer(fileIndexer, 2) :: ramIndexers
      

      ramIndexers.foreach(_.start)

      this.start
      accessManager ! RegisterListenerMsg(this)


      indexerTaskId = taskManager.submitTask(new BackgroundIndexTask(storageManager, this))



      indexScheduler.start


    }
  }

  @PreDestroy
  def destroy {

    try{
      indexScheduler.interrupt
    }catch{
      case e => log.warn("Exception while interrupting scheduler", e)
    }

    if(indexerTaskId != null){
      taskManager.stopTask(indexerTaskId)
    }

    if(searcher != null){
      searcher.close
      searcher = null
    }

    log info "Destroying SearchManager"
    accessManager ! UnregisterListenerMsg(this)

    configManager ! UnregisterMsg(this)

    for(ramIndexer <- ramIndexers){
      val exitValue = ramIndexer !? DestroyMsg
      log debug "Exit value for indexer is " + exitValue
    }

    fileIndexer ! DestroyMsg
    this ! DestroyMsg

  }

  def search(query: String): List[SearchResultEntry] = {

    log debug "Search called with query: " + query

    if(searcher == null){
      log debug "Searcher is null"
      List()
    }
    else searcher.search(query)
  }

  def act {
    while (true) {
      receive {
        case ResourceAddedMsg(resource) => selectIndexer ! IndexMsg(resource)

        case ResourceUpdatedMsg(resource) => fileIndexer ! DeleteForUpdateMsg(resource)

        case ResourceDeletedMsg(address) => fileIndexer ! DeleteMsg(address)

        case IndexMsg(resource) => selectIndexer ! IndexMsg(resource)

        case BackgroundIndexMsg(resource) =>
          fileIndexer ! DeleteForUpdateMsg(resource)
          statisticsManager ! IncreaseStatisticsMsg("c3.search.background", 1)

        case ResourceIndexedMsg(address) =>
          accessManager ! UpdateMetadataMsg(address, Map("indexed" -> new Date().getTime.toString))
          statisticsManager ! IncreaseStatisticsMsg("c3.search.indexed", 1)
        case DestroyMsg =>
          log info "Destroying SearchManager actor"
          this.exit
      }
    }
  }

  def flushIndexes {
    ramIndexers.foreach(_ ! FlushIndex(true))
  }

  def selectIndexer: RamIndexer = {
    log debug "Selecting indexer..."
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
        if(indexPath == null){
          indexPath = new Path(event.newValue)
          initialize
        }else{
          log warn "Search manager already initialized, new value will be used after system restart and all previous index will be lost!!!"
        }


      case INDEXER_COUNT => {
        val newCount = Integer.parseInt(event.newValue)

        log info "Ignoring new indexer count " + newCount

        /*if (ramIndexers.size < newCount) {
          for (i <- 1 to newCount - ramIndexers.size) {
            val indexer = new RamIndexer(fileIndexer)
            indexer.start
            ramIndexers = indexer :: ramIndexers
          }
        } else if (ramIndexers.size > newCount) {
          val dropCount = ramIndexers.size - newCount
          val toStop = ramIndexers.take(dropCount)
          ramIndexers = ramIndexers.drop(dropCount)

          toStop.foreach(_ ! DestroyMsg)
        }*/
      }

      case MAX_TMP_INDEX_SIZE =>
        if (event.newValue != event.oldValue)
          ramIndexers.foreach(_ ! SetMaxDocsCountMsg(Integer.parseInt(event.newValue)))
      case _ =>
    }
  }

}

class SearchIndexScheduler(val searchManager:SearchManagerImpl) extends Thread{

  val log = LogFactory.getLog(getClass)

  {
    this.setDaemon(true)
  }

  override def run{

    log info "Started scheduler"

    while(!Thread.currentThread.isInterrupted){
      try{
      Thread.sleep(1000 * 60)
      }catch{
        case e:InterruptedException =>
          log info "Thread interrupted"
          Thread.currentThread.interrupt
      }
      searchManager.ramIndexers.foreach(_ ! FlushIndex(false))
    }

    log info "Search scheduler stopped"
  }

}

case class BackgroundIndexMsg(val resource:Resource)