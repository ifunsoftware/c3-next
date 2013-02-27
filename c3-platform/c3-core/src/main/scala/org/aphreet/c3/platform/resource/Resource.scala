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

package org.aphreet.c3.platform.resource

import scala.collection.mutable.ArrayBuffer

import java.util.Date

import com.twmacinta.util.{MD5, MD5InputStream}
import java.io._
import collection.mutable
import SerializationUtil._

/**
 * Resource is an object that can be stored in the system.
 * Contains metadata, system metadata and array of versions
 */
class Resource {

  /**
   * Unique resource identifier. Assigned to resource after resource store
   */
  var address:String = null

  /**
   * Resource create data
   */
  var createDate = new Date

  /**
   * Map with user metadata
   */
  var metadata = new Metadata()

  /**
   * Map with system metadata
   */
  var systemMetadata = new Metadata()

  /**
   * Map with transient (non-persitent) metadata
   */
  var transientMetadata = new Metadata()

  /**
   * Array of versions
   */
  var versions = new ArrayBuffer[ResourceVersion]

  /**
   * Flag indicates if we need to store version history of this resource
   */
  var isVersioned = false

  var embedData = false

  /**
   * This method tries to return mime-type of resource
   */
  def mimeType:String = {
    if(metadata != null){
      metadata.get(Resource.MD_CONTENT_TYPE) match{
        case Some(t) => return t
        case None => null
      }
    }

    if(versions.size > 0){
      val data = versions(0).data
      if(data != null) return data.mimeType
    }

    Resource.MD_CONTENT_TYPE_DEFAULT
  }

  /**
   * Triggers checksum update in all not persisted resource versions
   * @see ResourceVersion
   */
  def calculateCheckSums = {
    if(!this.isVersioned){
      versions(0).calculateHash
    }else{
      versions.filter(!_.persisted).foreach(_.calculateHash)
    }
  }

  def resourceHash: Array[Byte] = {

    val bos = new ByteArrayOutputStream()

    val os = new DataOutputStream(bos)

    writeMetadata(metadata, os)
    writeMetadata(systemMetadata, os)

    val md5 = new MD5()

    md5.Update(bos.toByteArray)
    md5.Update(versions(0).data.hash)

    os.close()

    md5.Final()
  }

  def verifyCheckSums() {
    if(!this.isVersioned){
      versions(0).verifyCheckSum()
    }else{
      versions.filter(!_.persisted).foreach(_.verifyCheckSum())
    }
  }

  /**
   * Add new version to the resource
   */
  def addVersion(version:ResourceVersion){

    if(versions.size > 0){
      version.basedOnVersion = versions.last.date.getTime
    }else{
      version.basedOnVersion = 0L
    }

    if(!isVersioned){
      versions.clear()
    }
    versions += version
  }

  def lastUpdateDate:Date = {
    if(versions.isEmpty){
      createDate
    }else{
      versions.last.date
    }
  }

  /**
   * Serialize resource to byte array
   * Method simply writes all fields, metadata, system metadata to byte array.
   * Than it writes all versions to byte array (except data)
   */
  def toByteArray:Array[Byte] = {

    def writeDate(value:Date, dataOs:DataOutputStream) {
      var date = new Date(0)
      if(value != null) date = value
      dataOs.writeLong(date.getTime)
    }

    def writeVersions(versions:mutable.Buffer[ResourceVersion], dataOs:DataOutputStream) {
      dataOs.writeInt(1) //version class version
      dataOs.writeInt(versions.size)

      for(version <- versions.sortBy(_.date.getTime)){
        dataOs.writeInt(version.revision)
        writeDate(version.date, dataOs)
        dataOs.writeLong(version.basedOnVersion)

        if (embedData){
          version.systemMetadata.put(Resource.MD_DATA_LENGTH, version.data.length.toString)
        }

        writeMetadata(version.systemMetadata, dataOs)
        if (embedData){
          dataOs.writeLong(version.data.length)
          version.data.writeTo(dataOs)
        }
      }
    }


    val byteOs = new ByteArrayOutputStream
    val dataOs = new DataOutputStream(byteOs)

    dataOs.writeInt(3) //resource class version, for future

    dataOs.writeBoolean(isVersioned)

    writeString(address, dataOs)
    writeDate(createDate, dataOs)

    writeMetadata(metadata, dataOs)
    writeMetadata(systemMetadata, dataOs)

    dataOs.writeBoolean(embedData)

    writeVersions(versions, dataOs)

    val md5 = new MD5
    md5.Update(byteOs.toByteArray)

    byteOs.write(md5.Final)

    byteOs.toByteArray
  }

  override def clone:Resource = {

    val resource = new Resource
    resource.address = this.address
    resource.createDate = this.createDate.clone.asInstanceOf[Date]

    resource.metadata = this.metadata.clone()
    resource.systemMetadata = this.systemMetadata.clone()
    resource.transientMetadata = this.systemMetadata.clone()

    resource.isVersioned = this.isVersioned
    resource.embedData = this.embedData

    for(version <- this.versions){
      resource.versions.append(version.clone)
    }
    
    resource
  }

}

object Resource {

  /**
   * OBSOLETE. Now comparing md5 hash of the serialized resource
   * Last 8 bytes in the serialized resource version 1. If after deserialization they do not match
   * resource is considered to be broken
   */
  val STOP_SEQ : Long = 107533894376158093L

  val MD_CONTENT_TYPE = "content.type"
  val MD_DATA_ADDRESS = "c3.data.address"
  val MD_DATA_LENGTH = "c3.data.length"
  val MD_CONTENT_TYPE_DEFAULT = "application/octet-stream"
  val MD_TAGS = "c3.tags"
  val MD_USER = "c3.user"
  val SMD_LOCK = "c3.lock"
  val MD_UPDATED = "c3.updated"

  /**
   * String encoding for metadata storing
   */

  val MD_ENCODING = "UTF-8"

  /**
   * Deserialize resource from byte array.
   * Performs hashsum check or sequence end check.
   */
  def fromByteArray(bytes:Array[Byte]):Resource = {

    def readVersions(dataIs:DataInputStream, serializeVersion:Int, embedData:Boolean):mutable.Buffer[ResourceVersion] = {

      val result = new ArrayBuffer[ResourceVersion]

      val classVersion = dataIs.readInt //read version

      val count = dataIs.readInt

      for(i <- 1 to count){
        val version = new ResourceVersion
        version.revision = dataIs.readInt
        version.date = new Date(dataIs.readLong)

        if(classVersion == 0){
          version.basedOnVersion = 0L
        }else{
          version.basedOnVersion = dataIs.readLong
        }

        version.systemMetadata = readMetadata(dataIs)
        version.persisted = true

        if (embedData){
          val length = dataIs.readLong()
          val buffer = Array.ofDim[Byte](length.toInt)
          dataIs.read(buffer)

          version.data = DataStream.create(buffer)
        }

        result += version
      }

      result
    }


    val resource = new Resource

    val byteIn = new ByteArrayInputStream(bytes)

    val md5Is = new MD5InputStream(byteIn)

    val dataIn = new DataInputStream(md5Is)

    val serializeVersion = dataIn.readInt //resource class version

    resource.isVersioned = dataIn.readBoolean

    resource.address = readString(dataIn)
    resource.createDate = new Date(dataIn.readLong)

    resource.metadata = readMetadata(dataIn)
    resource.systemMetadata = readMetadata(dataIn)

    if(serializeVersion >= 3){
      resource.embedData = dataIn.readBoolean()
    }else{
      resource.embedData = false
    }

    resource.versions ++= readVersions(dataIn, serializeVersion, resource.embedData)

    serializeVersion match{
      case 0 => {
        val stopSeq = dataIn.readLong
        if(stopSeq != STOP_SEQ){
          throw new ResourceException("Failed to deserialize resource, wrong stop sequince")
        }
      }
      case _ => {
        val currentSumm = md5Is.getMD5.Final
        val savedSumm = new Array[Byte](16)

        dataIn.read(savedSumm)

        if(currentSumm.equals(savedSumm)){
          throw new ResourceException("Failed to deserialize resource, md5 hashes do not match")
        }
      }
    }

    dataIn.close()

    resource
  }
}
