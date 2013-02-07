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

import java.util.Date

import collection.mutable

/**
 * Representation of the resource version
 *
 */
class ResourceVersion{

  /**
   * Version create data
   */
  var date:Date = new Date

  /**
   * Reserved for future.
   */
  var revision:Int = 0

  /**
   * System metadata of the version
   */
  var systemMetadata = new mutable.HashMap[String, String]

  /**
   * Version's data
   */
  var data:DataStream = null

  /**
   * Flag indicates if resource has been written to storage
   */
  var persisted = false

  /**
   * Timestamp of the version's precessor
   */
  var basedOnVersion = 0L
  
  override def toString:String = {
    val builder = new StringBuilder

    builder.append(date.toString).append(" ").append(data.length).append(" ").append(revision)
    builder.append("\n\tMetadata:")

    for((key, value) <- systemMetadata){
      builder.append("\n\t\t").append(key).append(" => ").append(value)
    }

    builder.toString()
  }

  def setData(_data:DataStream) {data = _data}

  def calculateHash = {
    systemMetadata.put(ResourceVersion.RESOURCE_VERSION_HASH, data.hash)
  }

  def verifyCheckSum() {
    systemMetadata.get(ResourceVersion.RESOURCE_VERSION_HASH) match {
      case Some(value) => {
        if(value != data.hash) throw new ResourceException("Checksum verification failed")
      }
      case None => throw new ResourceException("Checksum verification failed")
    }
  }

  override def clone:ResourceVersion = {
    val version = new ResourceVersion
    version.persisted = this.persisted
    version.date = if(this.date != null) this.date.clone.asInstanceOf[Date] else null

    version.revision = this.revision
    version.systemMetadata = this.systemMetadata.clone()
    version.data = if(this.data != null) this.data.copy else null

    version
  }

}

object ResourceVersion{

  /**
   * The name of the field in the system metadata that store data's MD5 hash
   */
  val RESOURCE_VERSION_HASH = "c3.data.md5"

  def apply(stream:DataStream):ResourceVersion = {
    val version = new ResourceVersion
    version.data = stream
    version
  }
}
