/**
 * Copyright (c) 2011, Mikhail Malygin
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

package org.aphreet.c3.platform.test.system.storage

import junit.framework.TestCase
import junit.framework.Assert._
import org.aphreet.c3.platform.test.system.AbstractManagementTestCase
import org.aphreet.c3.platform.remote.api.management.{StorageDescription, PlatformManagementService}

class CreateStorageTestCase extends TestCase with AbstractManagementTestCase{

  var service:PlatformManagementService = null

  override def setUp{
    service = createManagementService
  }

  def testCreateStorage(){

    val oldStorages = service.listStorages

    service.createStorage("PureBDBStorage", localPath("data"))

    val foundStorages = service.listStorages


    var newStorages = List[StorageDescription]()

    for(storage <- foundStorages){
      if(!oldStorages.contains(storage)){
        newStorages = storage :: newStorages
      }
    }

    assertEquals("Expected only one new storage", 1, newStorages.size)

    val storage = newStorages.head

    assertEquals(storage.path, localPath("data"))
    assertEquals(storage.storageType, "PureBDBStorage")
    
    service.removeStorage(storage.id)

    newStorages = List[StorageDescription]()

    for(storage <- foundStorages){
      if(!oldStorages.contains(storage)){
        newStorages = storage :: newStorages
      }
    }

    assertEquals("Expected no new storages", 0, newStorages.size)


  }

}