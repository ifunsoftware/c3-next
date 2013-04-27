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
package org.aphreet.c3.platform.test.unit

import org.aphreet.c3.platform.common.Constants._
import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.resource._
import org.aphreet.c3.platform.storage._

import junit.framework.TestCase
import junit.framework.Assert._

class StorageModeSwitchTest extends TestCase{

  def testModeSwitchInUserMode() {
    
    val storage = new StorageStub
    
    storage.allowMoveFromModeToModes(RW(""), allUserModes ::: allMaintainModes)
    
    storage.allowMoveFromModeToModes(RW(""), allCapacityModes ::: allMaintainModes)
    
    storage.allowMoveFromModeToModes(RW(""), allMigrationModes ::: allMaintainModes)

    storage.denyMoveFromModeToModes(U(""), allUserModes ::: allMigrationModes ::: allCapacityModes)
    
    storage.denyMoveFromModeToModes(RO(STORAGE_MODE_CAPACITY), allUserModes ::: allTargetMigrationModes)
    
    storage.denyMoveFromModeToModes(RW(STORAGE_MODE_MIGRATION), allSourceMigrationModes ::: allUserModes ::: allCapacityModes)
    
    storage.denyMoveFromModeToModes(RO(STORAGE_MODE_MIGRATION), allUserModes)
    
    storage.denyMoveFromModeToModes(U(STORAGE_MODE_MIGRATION), RO(STORAGE_MODE_MIGRATION) :: allUserModes ::: allTargetMigrationModes ::: allCapacityModes)
    
    storage.allowMoveFromModeToModes(RW(STORAGE_MODE_MIGRATION), List(RW(STORAGE_MODE_NONE)) ::: allMaintainModes)
    
    storage.allowMoveFromModeToModes(RO(STORAGE_MODE_CAPACITY), List(RW(STORAGE_MODE_CAPACITY)) ::: allMaintainModes)

    storage.allowMoveFromModeToModes(U(STORAGE_MODE_MAINTAIN), allUserModes ::: allMigrationModes ::: allCapacityModes)

    storage.allowMoveFromModeToModes(RO(STORAGE_MODE_MAINTAIN), allUserModes ::: allMigrationModes ::: allCapacityModes)

    storage.allowMoveFromModeToModes(RW(STORAGE_MODE_MAINTAIN), allUserModes ::: allMigrationModes ::: allCapacityModes)


  }
  
  def allUserModes:List[StorageMode] =
    List(
      RO(STORAGE_MODE_USER), U(STORAGE_MODE_USER)
    )
  
  def allCapacityModes:List[StorageMode] = List(RO(STORAGE_MODE_CAPACITY))
  
  def allSourceMigrationModes:List[StorageMode] = 
    List(
      RO(STORAGE_MODE_MIGRATION), U(STORAGE_MODE_MIGRATION)
    )
  
  def allTargetMigrationModes:List[StorageMode] = List(RW(STORAGE_MODE_MIGRATION))
  
  def allMigrationModes:List[StorageMode] = allTargetMigrationModes ::: allSourceMigrationModes

  def allMaintainModes:List[StorageMode] =
    List(
      U(STORAGE_MODE_MAINTAIN), RW(STORAGE_MODE_MAINTAIN), RO(STORAGE_MODE_MAINTAIN)
      )
}

class StorageStub extends Storage{
  
  def id:String = ""
  
  def add(resource:Resource):String = ""
  
  def get(ra:String):Option[Resource] = None

  def update(resource:Resource):String = ""
  
  def delete(ra:String) {}
  
  def put(resource:Resource) {}

  def lock(ra:String) {}

  def unlock(ra:String) {}
  
  def params:StorageParams = null
  
  def count:Long = 0
  
  def usedCapacity:Long = 0

  def availableCapacity = 0L

  def iterator(fields:Map[String,String],
               systemFields:Map[String, String],
               filter:(Resource) => Boolean
          ):StorageIterator = null

  def iterator = iterator(Map(), Map(),(resource:Resource) => true)

  def close() {}
  
 
  def path:Path = null
  
  def fullPath:Path = null
  
  def name:String = ""

  def createIndex(index:StorageIndex) {}

  def removeIndex(index:StorageIndex) {}

  def appendMetadata(ra:String, metadata:Map[String, String], system: Boolean) {}
  
  def allowMoveFromModeToModes(initial:StorageMode, targetModes:List[StorageMode]){
    
    for(target <- targetModes){
      storageMode = initial
      try{
        this.mode = target
      }catch{
        case e: Throwable => {}
      }
      assertEquals(target, this.mode)
    }
  }
  
  def denyMoveFromModeToModes(initial:StorageMode, targetModes:List[StorageMode]){
    
    for(target <- targetModes){
      storageMode = initial
      try{
        this.mode = target
      }catch{
        case e: Throwable => {}
      }
      assertEquals(initial, this.mode)
    }
  }
}
