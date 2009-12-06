package org.aphreet.c3.platform.storage

import org.aphreet.c3.platform.common.Path

class StorageParams(val id:String, val secIds:List[String], val path:Path, val storageType:String, val mode:StorageMode){
  
  override def toString:String = {
    "Storage[id:" + id + " secIds:" + secIds + " path: " + path.toString + " storageType: " + storageType +  "]"
  }
  
  def containsId(checkedId:String):Boolean = 
    id.equals(checkedId) || secIds.exists(id => id.equals(checkedId))
  
}
