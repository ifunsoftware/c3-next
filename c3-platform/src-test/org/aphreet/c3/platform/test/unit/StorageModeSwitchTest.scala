package org.aphreet.c3.platform.test.unit

import org.aphreet.c3.platform.common.Constants._
import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.resource._
import org.aphreet.c3.platform.storage._

import junit.framework.TestCase
import junit.framework.Assert._

class StorageModeSwitchTest extends TestCase{

  def testModeSwitchInUserMode = {
    
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
  
  def delete(ra:String) = {}
  
  def put(resource:Resource) = {}
  
  
  def params:StorageParams = null
  
  def count:Long = 0
  
  def size:Long = 0
  
  def iterator:StorageIterator = null
  
  def close = {}
  
 
  def path:Path = null
  
  def fullPath:Path = null
  
  def name:String = ""
  
  def allowMoveFromModeToModes(initial:StorageMode, targetModes:List[StorageMode]){
    
    for(target <- targetModes){
      storageMode = initial
      try{
        this.mode = target
      }catch{
        case e => {}
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
        case e => {}
      }
      assertEquals(initial, this.mode)
    }
  }
}
