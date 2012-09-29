package org.aphreet.c3.platform.storage.composite

import org.aphreet.c3.platform.storage.StorageParams
import org.aphreet.c3.platform.storage.bdb.{AbstractSingleInstanceBDBStorage, BDBConfig}
import java.io.File

class CompositeStorage(override val parameters:StorageParams,
                     override val systemId:String,
                     override val config:BDBConfig)
          extends AbstractSingleInstanceBDBStorage(parameters, systemId, config)
          with CompositeDataManipulator{

  def name:String = CompositeStorage.NAME

  private val dataPath : File = {
    val path = new File(storagePath, "data")
    if(!path.exists) path.mkdirs
    path
  }

  override def getDataPath:File = dataPath
}

object CompositeStorage{
  val NAME = classOf[CompositeStorage].getSimpleName
}