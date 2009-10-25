package org.aphreet.c3.platform.storage

class StorageParams(val id:String, val secIds:List[String], val path:String, val storageType:String, val mode:StorageMode){
  
  override def toString:String = {
    "Storage[id:" + id + " secIds:" + secIds + " path: " + path + " storageType: " + storageType +  "]"
  }
  
  def containsId(checkedId:String):Boolean = 
    id.equals(checkedId) || secIds.exists(id => id.equals(checkedId))
  
}
