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

package org.aphreet.c3.platform.search.lucene.impl.background

import java.util.Date
import org.aphreet.c3.platform.query.QueryManager
import org.aphreet.c3.platform.resource.Resource
import org.aphreet.c3.platform.search.lucene.impl.SearchManagerInternal
import org.aphreet.c3.platform.task.IteratorTask

class BackgroundIndexTask(val queryManager: QueryManager, val searchManager: SearchManagerInternal, var indexCreateTimestamp: Long)
  extends IteratorTask[Resource](() => queryManager.contentIterator(Map(), Map())) {

  //We suppose that it should take no more than hour for resource to be indexed
  val indexedTimeout: Long = 1000 * 60 * 60

  override def throttle() {
    if (searchManager.throttleBackgroundIndex) {
      Thread.sleep(1000)
    }
  }

  def processElement(resource: Resource) {
    log trace "Checking resource " + resource.address
    if (shouldIndex(resource)) {
      log trace "Resource " + resource.address + " should be indexed"
      searchManager ! BackgroundIndexMsg(resource)
    }
  }

  override def preStart() {
    log info "Starting BackgroundIndexTask"
  }

  override def postComplete() {
    log info "Iteration is complete"
    searchManager ! BackgroundIndexRunCompletedMsg
  }

  private def shouldIndex(resource: Resource): Boolean = {

    def isOutOfTimeout(date: Date): Boolean = {
      System.currentTimeMillis - date.getTime > indexedTimeout
    }

    def isInPreviousIndex(indexedValue: String): Boolean = {
      try {
        indexedValue.toLong < indexCreateTimestamp
      } catch {
        case e: Throwable => false
      }
    }

    if (!resource.systemMetadata.has("c3.skip.index")) {
      resource.systemMetadata("indexed") match {
        case Some(x) => isInPreviousIndex(x)
        case None => isOutOfTimeout(resource.versions.last.date)
      }
    } else {
      false
    }
  }

  override def name = "BackgroundIndexer"
}

case class BackgroundIndexMsg(resource: Resource)

object BackgroundIndexRunCompletedMsg
