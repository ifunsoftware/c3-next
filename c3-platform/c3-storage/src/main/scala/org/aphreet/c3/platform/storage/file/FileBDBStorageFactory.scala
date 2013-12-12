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
package org.aphreet.c3.platform.storage.file

import org.aphreet.c3.platform.storage._
import org.aphreet.c3.platform.storage.bdb.AbstractBDBStorageFactory
import org.aphreet.c3.platform.config.{PlatformConfigComponent, PlatformConfigManager}
import org.aphreet.c3.platform.common.ComponentLifecycle
import org.aphreet.c3.platform.storage.StorageParams

trait FileBDBStorageComponent extends ComponentLifecycle{

  this: StorageComponent
    with PlatformConfigComponent =>

  val fileBDBStorageFactory = new FileBDBStorageFactory(storageManager, platformConfigManager)

  destroy(Unit => fileBDBStorageFactory.destroy())
}

class FileBDBStorageFactory(override val storageManager: StorageManager,
                            override val platformConfigManager: PlatformConfigManager)
  extends AbstractBDBStorageFactory(storageManager, platformConfigManager){

  protected def createNewStorage(params:StorageParams, systemId:String, conflictResolverProvider: ConflictResolverProvider):Storage =
    new FileBDBStorage(params, systemId, bdbConfig, conflictResolverProvider)

  def name:String = FileBDBStorage.NAME
  
}
