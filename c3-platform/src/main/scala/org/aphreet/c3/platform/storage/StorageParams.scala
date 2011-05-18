package org.aphreet.c3.platform.storage

import org.aphreet.c3.platform.common.Path
import collection.mutable.HashMap

case class StorageParams(val id : String,
                         val secIds : List[String],
                         val path : Path,
                         val storageType : String,
                         val mode : StorageMode,
                         val indexes:List[StorageIndex],
                         val params : HashMap[String, String]){
  
  def containsId(checkedId:String):Boolean = 
    id.equals(checkedId) || secIds.exists(id => id.equals(checkedId))
  
}

case class StorageIndex(val name:String,
                        val fields:List[String],
                        val multi:Boolean,
                        val system:Boolean,
                        val created:Long){

}
