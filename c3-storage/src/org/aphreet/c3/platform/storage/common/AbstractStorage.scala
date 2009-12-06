package org.aphreet.c3.platform.storage.common

import java.util.UUID

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.storage.StorageParams

abstract class AbstractStorage(val id:String, val path:Path) extends Storage{

  var secondaryIds:List[String] = List()
  
  var mode:StorageMode = U
  
  def generateName:String = {
    var address = UUID.randomUUID.toString + "-" + id
    
    while(isAddressExists(address)){
      address = UUID.randomUUID.toString + "-" + id
    }
    
    address
  }

  def ids:List[String] = secondaryIds
  
  def params:StorageParams = new StorageParams(id, ids, path, name, mode)
  
  def isAddressExists(address:String):Boolean
}
