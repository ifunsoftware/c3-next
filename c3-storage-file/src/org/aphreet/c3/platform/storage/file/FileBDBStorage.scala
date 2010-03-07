package org.aphreet.c3.platform.storage.file

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.resource.{DataWrapper, Resource, ResourceVersion}
import org.aphreet.c3.platform.storage.common.AbstractBDBStorage

import java.io._
import java.nio.channels.WritableByteChannel

import com.sleepycat.je._
import org.aphreet.c3.platform.exception.StorageException

class FileBDBStorage(override val id:String, override val path:Path) extends AbstractBDBStorage(id, path) {
  
  var dataPath : File = null  
  
  {
	dataPath = new File(storagePath, "data")
	if(!dataPath.exists) dataPath.mkdirs
  }
  
  override protected def storeData(resource:Resource){
    
    if(resource.isVersioned){
      for(version <- resource.versions if (version.persisted == false)){
	    val fileName = resource.address + ":" + String.valueOf(System.currentTimeMillis)
	    version.systemMetadata.put(Resource.MD_DATA_ADDRESS, fileName)
        storeVersionData(fileName, version)
	  }
    }else{
      val fileName = resource.address
      resource.versions(0).systemMetadata.put(Resource.MD_DATA_ADDRESS, fileName)
      storeVersionData(fileName, resource.versions(0))
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
      
      version.data = DataWrapper.wrap(getFileForRA(fileName))
    }
  }
  
  override protected def deleteData(ra:String){
    
    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry()
    
    if(database.get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS){
      val resource = Resource.fromByteArray(value.getData)
      
      for(version <- resource.versions){
        version.systemMetadata.get(Resource.MD_DATA_ADDRESS) match {
          case Some(name) => {
            try{
              getFileForRA(name).delete
            }catch{
              case e:IOException => throw new StorageException("Failed to delete file for ra: " + ra, e)
            }
          }
          case None => throw new StorageException("No data address in version for resource: " + ra)
        } 
      }
      
    }else throw new StorageException("Failed to get resource, operation status is not SUCCESS, address: " + ra)
  }
 
  
  def name:String = FileBDBStorage.NAME
  
  private def createFile(ra:String):File = {
    val file = getFullStoragePath(ra)
    
    file.getParentFile.mkdirs
    
    file
  }
  
  private def getFullStoragePath(name:String):File = {
    val dir0 = name charAt 0
    val dir1 = name charAt 1
    val dir2 = name charAt 2
    
    new File(dataPath, dir0 + File.separator + dir1 + File.separator + dir2 + File.separator + name)
  }
  
  private def getFileForRA(ra:String):File = {
    var file = getFullStoragePath(ra)
    
    if(!file.exists){
      file = new File(dataPath, ra)
      if(!file.exists){
        throw new StorageException("Can't find content with address :" + ra)
      }
    }
    file
  }
  
  private def storeVersionData(name:String, version:ResourceVersion){
      val targetFile : File = createFile(name)
      
      val channel : WritableByteChannel = new FileOutputStream(targetFile).getChannel()
    
      try{
    	version.data writeTo channel
    	version.data = DataWrapper.wrap(targetFile)
     
      }catch{
        case e:IOException => throw new StorageException("Failed to store data to file: " + targetFile.getAbsolutePath, e)
      }finally{
        if(channel != null && channel.isOpen)
    	    channel.close
      }
    }
}

object FileBDBStorage{
  val NAME = classOf[FileBDBStorage].getSimpleName
}
