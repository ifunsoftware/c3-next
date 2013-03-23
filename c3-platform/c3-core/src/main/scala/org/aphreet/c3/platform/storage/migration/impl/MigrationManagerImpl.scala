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
package org.aphreet.c3.platform.storage.migration.impl


import org.aphreet.c3.platform.common.{Logger, Constants}
import org.aphreet.c3.platform.exception.MigrationException
import org.aphreet.c3.platform.task.TaskManager

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.storage._
import dispatcher.impl.ZoneStorageDispatcher
import org.aphreet.c3.platform.storage.migration.MigrationManager

@Component
class MigrationManagerImpl extends MigrationManager{

  @Autowired
  var storageManager: StorageManager = null

  @Autowired
  var taskManager: TaskManager = null

  @Autowired
  var storageDispatcher: ZoneStorageDispatcher = null

  val log = Logger(getClass)

  def migrateStorageToStorage(sourceId: String, targetId: String) {
    val source = storageManager storageForId sourceId
    val target = storageManager storageForId targetId
    
    if(source != null && target != null){
      migrateStorageToStorage(source, target)
    }else{
      throw new MigrationException("Can't find one of storages")
    }
  }
  
  
  def migrateStorageToStorage(source: Storage, target: Storage) {
    
    log info "Starting migration from " + source.name + " " + source.id + " to " + target.name + " " + target.id
    
    try{
      
      checkPreconditions(source, target)
      source.mode = RO(Constants.STORAGE_MODE_MIGRATION)
      target.mode = RW(Constants.STORAGE_MODE_MIGRATION)
      
      val migration = new MigrationTask(source, target, storageManager)
      
      taskManager submitTask migration
      
    }catch{
      case e:MigrationException=> log.error(e.message, e)
      throw e
    }
  }
  
  private def checkPreconditions(source: Storage, target: Storage) {
  
    log info "Checking preconditions"
    
    if(!source.mode.allowRead){
      throw new MigrationException("Source is not readable")
    }
    
    if(!target.mode.allowWrite){
      throw new MigrationException("Target is not writable")
    }
    
    val requiredFreeCapacity = source.usedCapacity

    if(target.availableCapacity < source.usedCapacity){
      throw new MigrationException("Not enough capacity on target storage")
    }
    
    log info "Preconditions check complete"
  }
}
