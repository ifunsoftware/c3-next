package org.aphreet.c3.platform.storage

import org.aphreet.c3.platform.common.Path

case class StorageParams(val id:String, val secIds:List[String], val path:Path, val storageType:String, val mode:StorageMode){
  
  def containsId(checkedId:String):Boolean = 
    id.equals(checkedId) || secIds.exists(id => id.equals(checkedId))
  
}
