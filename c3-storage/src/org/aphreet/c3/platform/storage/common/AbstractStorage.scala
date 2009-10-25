package org.aphreet.c3.platform.storage.common

import java.util.UUID
import org.aphreet.c3.platform.storage.StorageParams

abstract class AbstractStorage(val id:String, val path:String) extends Storage{

  var secondaryIds:List[String] = List()
  
  var mode:StorageMode = U
  
  def generateName:String = UUID.randomUUID.toString + "-" + id

  def ids:List[String] = secondaryIds
  
  def params:StorageParams = new StorageParams(id, ids, path, name, mode)
}
