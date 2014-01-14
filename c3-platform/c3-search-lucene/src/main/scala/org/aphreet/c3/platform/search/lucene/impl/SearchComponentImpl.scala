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
package org.aphreet.c3.platform.search.lucene.impl

import background._
import index._
import search._
import java.io.File
import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.common.msg._
import org.aphreet.c3.platform.common.{ComponentLifecycle, Logger, ComponentGuard, Path}
import org.aphreet.c3.platform.config._
import org.aphreet.c3.platform.search.api._
import org.aphreet.c3.platform.statistics.{StatisticsComponent, IncreaseStatisticsMsg}
import org.aphreet.c3.platform.storage.StorageComponent
import org.aphreet.c3.platform.task.TaskComponent
import akka.actor.{Actor, Props}
import org.aphreet.c3.platform.search.lucene.DropFieldConfiguration
import org.aphreet.c3.platform.actor.ActorComponent
import org.aphreet.c3.platform.search.lucene.impl.index.extractor.TikaHttpTextExtractor

trait SearchComponentImpl extends SearchComponent{

  this: AccessComponent
    with ActorComponent
    with StorageComponent
    with PlatformConfigComponent
    with TaskComponent
    with StatisticsComponent
    with ComponentLifecycle =>

  val searchConfigurationManager = new SearchConfigurationManagerImpl(actorSystem,
    new SearchConfigurationAccessor(configPersister))

  val searchManager = new SearchManagerImpl()

  class SearchManagerImpl extends SearchManager with SearchManagerInternal with SPlatformPropertyListener with ComponentGuard {

    import SearchComponentConstants._

    val log = Logger(getClass)

    var fileIndexer: FileIndexer = null

    var indexPath: Path = null

    var ramIndexers: List[RamIndexer] = List()

    val random = new java.util.Random(System.currentTimeMillis)

    var searcher:Searcher = null

    val indexScheduler = new SearchIndexScheduler(this)

    var indexerTaskId:String = null

    var backgroundIndexTask:BackgroundIndexTask = null

    var throttleBackgroundIndexer: Boolean = true

    var indexCreateTimestamp = 0l

    var extractDocumentContent = false

    var currentTikaAddress: String = null

    val async = actorSystem.actorOf(Props.create(classOf[SearchManagerActor], this))

    def tikaHostAddress = if(currentTikaAddress == null) defaultValues.get(TIKA_HOST).get else currentTikaAddress

    {

      if (indexPath != null) {
        initialize(INDEXER_COUNT)
      } else {
        log warn "Index path is not set. Waiting for property to appear"
      }

      platformConfigManager ! RegisterMsg(this)
    }

    def initialize(numberOfIndexers:Int) {
      if (fileIndexer == null) {

        for(i <- 1 to numberOfIndexers){
          ramIndexers = createIndexer(i) :: ramIndexers
        }

        searcher = new Searcher(actorSystem, searchConfigurationManager)

        fileIndexer = new FileIndexer(indexPath, searcher)

        accessMediator ! RegisterNamedListenerMsg(async, 'SearchManager)

        backgroundIndexTask = new BackgroundIndexTask(storageManager, this, indexCreateTimestamp)

        taskManager.scheduleTask(backgroundIndexTask, INDEX_DELAY, START_INDEX_DELAY, fixedPeriod = false)
        indexerTaskId = backgroundIndexTask.id

        indexScheduler.start()

        searcher ! NewIndexPathMsg(indexPath)
      }
    }

    def search(domain:String, query: String): SearchResult = {

      log debug "Search called with query: " + query

      if(searcher == null){
        log debug "Searcher is null"
        SearchResult(query, new Array[SearchResultElement](0))
      }
      else searcher.search(domain, query)
    }

    class SearchManagerActor extends Actor{

      def receive = {
        case ResourceAddedMsg(resource, source) => selectIndexer ! IndexMsg(resource)

        case ResourceUpdatedMsg(resource, source) => {
          fileIndexer.deleteForIndex(resource, self)
        }

        case ResourceDeletedMsg(address, source) => fileIndexer.delete(address)

        case IndexMsg(resource) => selectIndexer ! IndexMsg(resource)

        case BackgroundIndexMsg(resource) =>
          fileIndexer.deleteForIndex(resource, self)
          statisticsManager ! IncreaseStatisticsMsg("c3.search.background", 1)

        case ResourceIndexingFailed(address) =>
          statisticsManager ! IncreaseStatisticsMsg("c3.search.failed", 1)

        case BackgroundIndexRunCompletedMsg =>
          statisticsManager ! IncreaseStatisticsMsg("c3.search.background.runs", 1)

        case ResourceIndexedMsg(address, extractedMetadata) =>
          accessManager ! UpdateMetadataMsg(address, Map("indexed" -> System.currentTimeMillis.toString), system=true)

          if(!extractedMetadata.isEmpty){
            accessManager ! UpdateMetadataMsg(address, extractedMetadata, system=false)
          }

          statisticsManager ! IncreaseStatisticsMsg("c3.search.indexed", 1)

        case UpdateIndexCreationTimestamp(time) => //Update timestamp in the background indexer task
          platformConfigManager.setPlatformProperty(INDEX_CREATE_TIMESTAMP, time.toString)

        case StoragePurgedMsg(source) => deleteIndexes()
      }

      override def postStop(){
        log info "Destroying SearchManager actor"

        letItFall{

          if(indexerTaskId != null){
            taskManager.stopTask(indexerTaskId)
          }

          accessMediator ! UnregisterNamedListenerMsg(self, 'SearchManager)
          platformConfigManager ! UnregisterMsg(SearchManagerImpl.this)
        }


        indexScheduler.interrupt()

        fileIndexer.destroy()
      }
    }

    def deleteIndexes(){
      log.info("Reseting search index")
      fileIndexer.deleteIndex()
      searchConfigurationManager ! DropFieldConfiguration
      async ! UpdateIndexCreationTimestamp(System.currentTimeMillis())
    }

    def flushIndexes() {
      ramIndexers.foreach(_ ! FlushIndex(force = true))
    }

    def dumpIndex(path: String) {
      taskManager.submitTask(new DumpIndexTask(indexPath, path))
    }

    def selectIndexer: RamIndexer = {
      log trace "Selecting indexer..."
      val num = math.abs(random.nextInt) % ramIndexers.size
      ramIndexers.drop(num).head
    }

    def defaultValues: Map[String, String] = Map(
      MAX_TMP_INDEX_SIZE -> "100",
      INDEX_CREATE_TIMESTAMP -> "0",
      EXTRACT_DOCUMENT_CONTENT -> "false",
      INDEX_PATH -> new File(platformConfigManager.dataDir, "index").getAbsolutePath,
      TIKA_HOST -> "https://tika-ifunsoftware.rhcloud.com",
      THROTTLE_BACKGROUND_INDEX -> "true"
    )

    override def listeningForProperties: Array[String] = Array(
      INDEX_PATH, MAX_TMP_INDEX_SIZE, INDEX_CREATE_TIMESTAMP, EXTRACT_DOCUMENT_CONTENT, TIKA_HOST, THROTTLE_BACKGROUND_INDEX
    )

    def propertyChanged(event: PropertyChangeEvent) {
      event.name match {
        case INDEX_PATH => {
          val newPath = new Path(event.newValue)

          if(indexPath == null){
            log info "Found path to store index: " + newPath.stringValue
            indexPath = newPath
            initialize(INDEXER_COUNT)
          }else{

            if(newPath != indexPath){
              log info "New path to store index set: " + newPath.stringValue
              fileIndexer.updateIndexLocation(newPath, async)
              indexPath = newPath
            }else{
              log info "New index path is the same as existing"
            }
          }
        }


        case MAX_TMP_INDEX_SIZE =>
          if (event.newValue != event.oldValue)
            ramIndexers.foreach(_ ! SetMaxDocsCountMsg(Integer.parseInt(event.newValue)))

        case INDEX_CREATE_TIMESTAMP =>
          log info "Index creation timestamp value: " + event.newValue
          indexCreateTimestamp = event.newValue.toLong
          if(backgroundIndexTask != null)
            backgroundIndexTask.indexCreateTimestamp = indexCreateTimestamp

        case EXTRACT_DOCUMENT_CONTENT =>
          log info "Setting " + EXTRACT_DOCUMENT_CONTENT + " value: " + event.newValue
          extractDocumentContent = event.newValue == "true"

          for(indexer <- ramIndexers){
            indexer.extractDocumentContent = extractDocumentContent
          }

        case TIKA_HOST =>
          log info "Setting tika host to " + event.newValue
          currentTikaAddress = event.newValue
          ramIndexers.foreach(_ ! UpdateTextExtractor(new TikaHttpTextExtractor(event.newValue)))

        case THROTTLE_BACKGROUND_INDEX =>
          log info "Setting " + THROTTLE_BACKGROUND_INDEX + " to " + event.newValue
          throttleBackgroundIndexer = event.newValue.toBoolean
      }
    }

    def throttleBackgroundIndex = {
      throttleBackgroundIndexer
    }

    protected def createIndexer(number: Int): RamIndexer = {
      new RamIndexer(actorSystem, fileIndexer,
        searchConfigurationManager,
        number,
        extractDocumentContent,
        new TikaHttpTextExtractor(tikaHostAddress))
    }
  }

  class SearchIndexScheduler(val searchManager:SearchManagerImpl) extends Thread{

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
        searchManager.ramIndexers.foreach(_ ! FlushIndex(force = false))
      }

      log info "Search scheduler stopped"
    }
  }
}

object SearchComponentConstants{

  val INDEXER_COUNT = 4

  val INDEX_PATH = "c3.search.index.path"

  val MAX_TMP_INDEX_SIZE = "c3.search.index.max_size"

  val INDEX_CREATE_TIMESTAMP = "c3.search.index.create_timestamp"

  val EXTRACT_DOCUMENT_CONTENT = "c3.search.index.extract_content"

  val TIKA_HOST = "c3.search.index.tika_address"

  val THROTTLE_BACKGROUND_INDEX = "c3.search.index.throttle_background_index"

  val START_INDEX_DELAY : Long = 3 * 60 * 1000

  val INDEX_DELAY : Long = 60 * 60 * 1000

}
