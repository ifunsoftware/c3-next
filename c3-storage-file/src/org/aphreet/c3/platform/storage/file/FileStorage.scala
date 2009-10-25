package org.aphreet.c3.platform.storage.file

import org.aphreet.c3.platform.resource.{DataWrapper, Resource}

import org.aphreet.c3.platform.storage.common.AbstractBDBStorage

import org.aphreet.c3.platform.storage._

import java.io._
import java.nio.channels.WritableByteChannel

class FileStorage(override val id:String, override val path:String) extends AbstractBDBStorage(id, path){
  
  var dataPath : File = null  
  
  {
	dataPath = new File(storagePath, "data");
	if(!dataPath.exists) dataPath.mkdirs
  }
  
  def storageType:StorageType.Value = StorageType.FIXED
  
  override protected def storeData(ra:String, data:DataWrapper){
    
    val targetFile : File = createFile(ra)  
    val channel : WritableByteChannel = new FileOutputStream(targetFile).getChannel()
    
    try{
    	
    	data writeTo channel
    	
    }catch{
      case e:IOException => throw new StorageException("Failed to store data to file: " + targetFile.getAbsolutePath, e)
    }finally{
      if(channel != null)
    	  channel.close
    }
  }
  
  def fillResourceWithData(resource:Resource) = {
    resource.data = DataWrapper.wrap(getFileForRA(resource.address))
  }
  
  override protected def deleteData(ra:String){
    try{
    	getFileForRA(ra).delete
    }catch{
      case e:IOException => throw new StorageException("Failed to delete file for ra: " + ra)
    }
  }
 
  
  def name:String = FileStorage.NAME
  
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
}

object FileStorage{
  val NAME = classOf[FileStorage].getSimpleName
}
