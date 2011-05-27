package org.aphreet.c3.platform.storage.file

import java.io._
import org.aphreet.c3.platform.storage.StorageParams
import org.aphreet.c3.platform.storage.bdb.{AbstractSingleInstanceBDBStorage, BDBConfig}

class FileBDBStorage(override val parameters:StorageParams,
                     override val systemId:String,
                     override val config:BDBConfig)
          extends AbstractSingleInstanceBDBStorage(parameters, systemId, config)
          with FileDataManipulator{

  def name:String = FileBDBStorage.NAME

  private val dataPath : File = {
    val path = new File(storagePath, "data")
    if(!path.exists) path.mkdirs
    path
  }

  override def getDataPath:File = dataPath
  
}

object FileBDBStorage{
  val NAME = classOf[FileBDBStorage].getSimpleName
}
