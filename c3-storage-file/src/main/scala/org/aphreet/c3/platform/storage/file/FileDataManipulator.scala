/**
 * Copyright (c) 2010, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.aphreet.c3.platform.storage.file

import com.sleepycat.je.{OperationStatus, LockMode, DatabaseEntry}
import java.io.{IOException, File}
import org.aphreet.c3.platform.resource.{Resource, ResourceVersion, DataStream}
import org.aphreet.c3.platform.exception.{StorageException, ResourceNotFoundException}
import org.aphreet.c3.platform.storage.bdb.{DatabaseProvider, DataManipulator}

trait FileDataManipulator extends DataManipulator with DatabaseProvider{

  def getDataPath:File

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

      version.data = DataStream.create(findFileForName(fileName))
    }
  }

  override protected def deleteData(ra:String){

    val key = new DatabaseEntry(ra.getBytes)
    val value = new DatabaseEntry()

    val status = getDatabase(true).get(null, key, value, LockMode.DEFAULT)

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
      version.data = DataStream.create(targetFile)
      version.systemMetadata.put(Resource.MD_DATA_LENGTH, version.data.length.toString)

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

    new File(getDataPath, dir0 + File.separator + dir1 + File.separator + dir2 + File.separator + name)
  }
}