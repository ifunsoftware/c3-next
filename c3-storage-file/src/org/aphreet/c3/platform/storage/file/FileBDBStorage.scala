package org.aphreet.c3.platform.storage.file

import org.aphreet.c3.platform.resource.{DataWrapper, Resource, ResourceVersion}
import java.io._
import com.sleepycat.je._
import org.aphreet.c3.platform.exception.{ResourceNotFoundException, StorageException}
import org.aphreet.c3.platform.storage.common.{BDBConfig, AbstractBDBStorage}
import org.aphreet.c3.platform.storage.StorageParams

class FileBDBStorage(override val parameters:StorageParams,
                     override val systemId:String,
                     override val config:BDBConfig) extends AbstractBDBStorage(parameters, systemId, config) {

  var dataPath : File = null

  {
    dataPath = new File(storagePath, "data")
    if(!dataPath.exists) dataPath.mkdirs
  }

  def name:String = FileBDBStorage.NAME

  override protected def storeData(resource:Resource){

    if(resource.isVersioned){
      for(version <- resource.versions if (version.persisted == false)){
        val fileName = resource.address + "-" + String.valueOf(System.currentTimeMillis) + "-" + version.data.hash
        version.systemMetadata.put(Resource.MD_DATA_ADDRESS, fileName)
        storeVersionData(fileName, version)
      }
    }else{
      val version = resource.versions(0)
      if(!version.persisted){
        val fileName = resource.address
        version.systemMetadata.put(Resource.MD_DATA_ADDRESS, fileName)
        storeVersionData(fileName, version)
      }
    }
  }

  override protected def putData(resource:Resource){
    for(version <- resource.versions){
      val fileName = version.systemMetadata.get(Resource.MD_DATA_ADDRESS) match {
        case Some(name) => name
        case None => throw new StorageException("Can't find data address for version")
      }
      storeVersionData(fileName, version)
    }
  }

  def loadData(resource:Resource) = {

    for(version <- resource.versions){

      val fileName = version.systemMetadata.get(Resource.MD_DATA_ADDRESS) match {
        case Some(value:String) => value
        case None => throw new StorageException("Can't find data reference for version in resource: " + resource.address)
      }

      version.data = DataWrapper.wrap(findFileForName(fileName))
    }
  }

  override protected def deleteData(ra:String){

    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry()

    val status = database.get(null, key, value, LockMode.DEFAULT)

    if(status == OperationStatus.SUCCESS){
      val resource = Resource.fromByteArray(value.getData)

      for(version <- resource.versions){
        version.systemMetadata.get(Resource.MD_DATA_ADDRESS) match {
          case Some(name) => {
            try{
              findFileForName(name).delete
            }catch{
              case e:IOException => throw new StorageException("Failed to delete file for ra: " + ra, e)
            }
          }
          case None => throw new StorageException("No data address in version for resource: " + ra)
        }
      }

    }else throw new ResourceNotFoundException(ra)
  }



  private def findFileForName(ra:String):File = {
    val file = buildFileForName(ra)

    if(!file.exists){
        throw new StorageException("Can't find content with address :" + ra)
    }
    file
  }

  private def storeVersionData(name:String, version:ResourceVersion){

    val targetTempFile = createTempFile(name)

    val targetFile : File = createDataFile(name)

    try{

      version.data writeTo targetTempFile

      targetTempFile.renameTo(targetFile)

      //version.data writeTo targetFile
      version.data = DataWrapper.wrap(targetFile)

    }catch{
      case e:IOException => throw new StorageException("Failed to store data to file: " + targetFile.getAbsolutePath, e)
    }
  }

  private def createTempFile(name:String):File = {
    createDataFile(name + "_" + System.currentTimeMillis)
  }

  private def createDataFile(name:String):File = {
    val file = buildFileForName(name)

    file.getParentFile.mkdirs

    file
  }

  private def buildFileForName(name:String):File = {
    val dir0 = name charAt 0
    val dir1 = name charAt 1
    val dir2 = name charAt 2

    new File(dataPath, dir0 + File.separator + dir1 + File.separator + dir2 + File.separator + name)
  }
}

object FileBDBStorage{
  val NAME = classOf[FileBDBStorage].getSimpleName
}
