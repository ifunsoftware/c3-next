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
package org.aphreet.c3.platform.storage.common


import org.aphreet.c3.platform.common.{Logger, ComponentGuard}
import org.aphreet.c3.platform.storage._
import scala.collection.mutable


abstract class AbstractStorageFactory(val storageManager: StorageManager) extends StorageFactory with ComponentGuard {

  val log = Logger(getClass)

  val createdStorages = new mutable.HashSet[Storage]

  def createStorage(params: StorageParams, systemId: String, conflictResolverProvider: ConflictResolverProvider): Storage = {
    val storage = createNewStorage(params, systemId, conflictResolverProvider)

    storage.mode = params.mode

    createdStorages += storage
    storage
  }

  def storages: mutable.Set[Storage] = createdStorages

  protected def createNewStorage(params: StorageParams, systemId: String, conflictResolverProvider: ConflictResolverProvider): Storage

  def init() {
    log info "Starting " + this.name + " storage factory"
    storageManager.registerFactory(this)
  }

  def destroy() {
    log info "Stopping " + this.name + " storage factory"

    createdStorages.foreach(s => s.close())

    letItFall {
      storageManager.unregisterFactory(this)
    }

    createdStorages.clear()
  }
}
