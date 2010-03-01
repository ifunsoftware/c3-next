package org.aphreet.c3.platform.storage.migration.impl

import org.aphreet.c3.platform.common.Constants._
import org.aphreet.c3.platform.storage._
import org.aphreet.c3.platform.task.Task
class MigrationTask(val source:Storage, val target:Storage, val manager:StorageManager) extends Task{

  var iterator:StorageIterator = _
  
  override def preStart = {
    iterator = source.iterator
  }
  
  override def step = {
    val resource = iterator.next
    target.put(resource)
  }
  
  override def postComplete = {
    iterator.close
    iterator = null
    
    target.ids = source.id :: source.ids ::: target.ids
    target.mode = new RW
    manager updateStorageParams target
    
    source.mode = new U(STORAGE_MODE_MIGRATION)
    manager removeStorage source
  }
  
  override def postFailure = {
    try{
      iterator.close
      iterator = null
    }catch{
      case e=> log error e
    }
    
    target.mode = new RW
    source.mode = new RW
    
    manager updateStorageParams source
    manager updateStorageParams target
  }
  
  override def shouldStop:Boolean = !iterator.hasNext
  
  override def progress:Int = {
    if(iterator != null){
      val toProcess:Float = iterator.objectsProcessed
      val total:Float = source.count

      if(total > 0){
        val overalProgress = toProcess * 100 /total
        overalProgress.intValue
      }else{
        0
      }
     }else -1
  }
  
  override def finalize = {
    if(iterator != null)
      try{
        iterator.close
      }catch{
        case e => e.printStackTrace
      }
  }
}
