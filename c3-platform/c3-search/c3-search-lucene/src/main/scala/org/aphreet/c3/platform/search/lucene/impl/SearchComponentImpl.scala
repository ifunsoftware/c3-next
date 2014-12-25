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
import org.aphreet.c3.platform.search.lucene.impl.SearchComponentProtocol.{UpdateIndexCreationTimestamp, ResourceIndexedMsg, ResourceIndexingFailed}
import org.aphreet.c3.platform.query.QueryComponent

trait SearchComponentImpl extends SearchComponent {

  this: AccessComponent
    with ActorComponent
    with StorageComponent
    with PlatformConfigComponent
    with TaskComponent
    with StatisticsComponent
    with QueryComponent
    with ComponentLifecycle =>

  val searchConfigurationManager = new SearchConfigurationManagerImpl(actorSystem,
    new SearchConfigurationAccessor(configPersister))

  val searchManager = new SearchManagerImpl()

  class SearchManagerImpl extends SearchManager with SearchManagerInternal with SPlatformPropertyListener with ComponentGuard {

    import SearchComponentConstants._

    val log = Logger(getClass)

    var fileIndexHolder: FileIndexHolder = null

    var indexPath: Path = null

    var indexer: ResourceIndexer = null

    var searcher: Searcher = null

    var backgroundIndexTask: BackgroundIndexTask = null

    var throttleBackgroundIndexer: Boolean = true

    var indexCreateTimestamp = 0l

    var extractDocumentContent = false

    var currentTikaAddress: Option[String] = None

    val async = actorSystem.actorOf(Props.create(classOf[SearchManagerActor], this))

    def tikaHostAddress: String = currentTikaAddress.getOrElse(defaultValues.get(TIKA_HOST).get)

    {
      if (indexPath != null) {
        initialize(INDEXER_COUNT)
      } else {
        log warn "Index path is not set. Waiting for property to appear"
      }

      platformConfigManager ! RegisterMsg(this)
    }

    def initialize(numberOfIndexers: Int) {
      if (fileIndexHolder == null) {
        searcher = new Searcher(actorSystem, searchConfigurationManager)

        fileIndexHolder = new FileIndexHolder(indexPath, searcher)

        indexer = new ParallelResourceIndexer(20, fileIndexHolder,
          searchConfigurationManager,
          extractDocumentContent,
          new TikaHttpTextExtractor(tikaHostAddress))

        accessMediator ! RegisterNamedListenerMsg(async, 'SearchManager)

        backgroundIndexTask = new BackgroundIndexTask(queryManager, this, indexCreateTimestamp)

        taskManager.scheduleTask(backgroundIndexTask, INDEX_DELAY, START_INDEX_DELAY, fixedPeriod = false)

        searcher ! NewIndexPathMsg(indexPath)
      }
    }

    def search(domain: String, query: String): SearchResult = {

      log debug "Search called with query: " + query

      if (searcher == null) {
        log debug "Searcher is null"
        SearchResult(query, new Array[SearchResultElement](0))
      }
      else searcher.search(domain, query)
    }

    class SearchManagerActor extends Actor {

      def receive = {
        case ResourceAddedMsg(resource, source) => indexer.index(resource, self)

        case ResourceUpdatedMsg(resource, source) => indexer.index(resource, self)

        case ResourceDeletedMsg(address, source) => indexer.delete(address)

        case BackgroundIndexMsg(resource) =>
          indexer.index(resource, self)
          statisticsManager ! IncreaseStatisticsMsg("c3.search.background", 1)

        case ResourceIndexingFailed(address) =>
          statisticsManager ! IncreaseStatisticsMsg("c3.search.failed", 1)

        case BackgroundIndexRunCompletedMsg =>
          statisticsManager ! IncreaseStatisticsMsg("c3.search.background.runs", 1)

        case ResourceIndexedMsg(address, extractedMetadata) =>
          accessManager ! UpdateMetadataMsg(address, Map("indexed" -> System.currentTimeMillis.toString), system = true)

          if (extractedMetadata.nonEmpty) {
            accessManager ! UpdateMetadataMsg(address, extractedMetadata, system = false)
          }

          statisticsManager ! IncreaseStatisticsMsg("c3.search.indexed", 1)

        case UpdateIndexCreationTimestamp(time) => //Update timestamp in the background indexer task
          platformConfigManager.setPlatformProperty(INDEX_CREATE_TIMESTAMP, time.toString)

        case StoragePurgedMsg(source) => deleteIndexes()
      }

      override def postStop() {
        log info "Destroying SearchManager actor"

        letItFall {

          if (backgroundIndexTask != null) {
            taskManager.stopTask(backgroundIndexTask.id)
          }

          accessMediator ! UnregisterNamedListenerMsg(self, 'SearchManager)
          platformConfigManager ! UnregisterMsg(SearchManagerImpl.this)
        }

        indexer.destroy()
        fileIndexHolder.destroy()
      }
    }

    def deleteIndexes() {
      log.info("Reseting search index")
      fileIndexHolder.deleteIndex(async)
      searchConfigurationManager ! DropFieldConfiguration
    }

    def dumpIndex(path: String) {
      taskManager.submitTask(new DumpIndexTask(indexPath, path))
    }

    def defaultValues: Map[String, String] = Map(
      INDEX_CREATE_TIMESTAMP -> "0",
      EXTRACT_DOCUMENT_CONTENT -> "false",
      INDEX_PATH -> new File(platformConfigManager.dataDir, "index").getAbsolutePath,
      TIKA_HOST -> "https://tika-ifunsoftware.rhcloud.com",
      THROTTLE_BACKGROUND_INDEX -> "true"
    )

    override def listeningForProperties: Array[String] = Array(
      INDEX_PATH, INDEX_CREATE_TIMESTAMP, EXTRACT_DOCUMENT_CONTENT, TIKA_HOST, THROTTLE_BACKGROUND_INDEX
    )

    def propertyChanged(event: PropertyChangeEvent) {
      event.name match {
        case INDEX_PATH =>
          val newPath = new Path(event.newValue)

          if (indexPath == null) {
            log info "Found path to store index: " + newPath.stringValue
            indexPath = newPath
            initialize(INDEXER_COUNT)
          } else {

            if (newPath != indexPath) {
              log info "New path to store index set: " + newPath.stringValue
              fileIndexHolder.updateIndexLocation(newPath, async)
              indexPath = newPath
            } else {
              log info "New index path is the same as existing"
            }
          }

        case INDEX_CREATE_TIMESTAMP =>
          log info "Index creation timestamp value: " + event.newValue
          indexCreateTimestamp = event.newValue.toLong
          if (backgroundIndexTask != null)
            backgroundIndexTask.indexCreateTimestamp = indexCreateTimestamp

        case EXTRACT_DOCUMENT_CONTENT =>
          log info "Setting " + EXTRACT_DOCUMENT_CONTENT + " value: " + event.newValue
          extractDocumentContent = event.newValue == "true"

          indexer.setDocumentExtractionRequired(extractDocumentContent)

        case TIKA_HOST =>
          log info "Setting tika host to " + event.newValue
          currentTikaAddress = Some(event.newValue)
          indexer.updateTextExtractor(new TikaHttpTextExtractor(event.newValue))

        case THROTTLE_BACKGROUND_INDEX =>
          log info "Setting " + THROTTLE_BACKGROUND_INDEX + " to " + event.newValue
          throttleBackgroundIndexer = event.newValue.toBoolean
      }
    }

    def throttleBackgroundIndex = {
      throttleBackgroundIndexer
    }
  }

}

object SearchComponentProtocol {

  case class ResourceIndexingFailed(address: String)

  case class ResourceIndexedMsg(address: String, extractedMetadata: Map[String, String])

  case class UpdateIndexCreationTimestamp(time: Long)

}

object SearchComponentConstants {

  val INDEXER_COUNT = 4

  val INDEX_PATH = "c3.search.index.path"

  val MAX_TMP_INDEX_SIZE = "c3.search.index.max_size"

  val INDEX_CREATE_TIMESTAMP = "c3.search.index.create_timestamp"

  val EXTRACT_DOCUMENT_CONTENT = "c3.search.index.extract_content"

  val TIKA_HOST = "c3.search.index.tika_address"

  val THROTTLE_BACKGROUND_INDEX = "c3.search.index.throttle_background_index"

  val START_INDEX_DELAY: Long = 3 * 60 * 1000

  val INDEX_DELAY: Long = 60 * 60 * 1000

}
