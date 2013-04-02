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
package org.aphreet.c3.platform.remote.replication.impl.data

import java.nio.channels.Channels
import java.io._
import org.apache.commons.httpclient.methods.GetMethod
import org.aphreet.c3.platform.exception.StorageException
import org.aphreet.c3.platform.resource.{DataStream, AbstractFileDataStream}
import org.aphreet.c3.platform.remote.api.management.ReplicationHost
import org.apache.commons.httpclient.{Header, HttpMethodBase, HttpClient, HttpStatus}
import java.text.SimpleDateFormat
import java.util.Date
import org.aphreet.c3.platform.auth.HashUtil
import org.aphreet.c3.platform.common.Logger

class RemoteSystemDataStream(val host:ReplicationHost,
                              val secure:Boolean,
                              val address:String,
                              val version:Int,
                              val domainId:String,
                              val domainKey:String) extends AbstractFileDataStream{

  private var created = false

  override def getFile:File = {

    val file:File = File.createTempFile(address, version.toString)

    file.deleteOnExit()

    created = true

    val requestUri = "/rest/resource/" + address + "/" + version

    val getMethod = new GetMethod(host.httpServerString(secure) + requestUri)

    val header = new Header("x-c3-domain", domainId)
    getMethod.addRequestHeader(header)

    //Check if we work with anonymous domain
    if(!domainKey.isEmpty){
      addAuthHeader(getMethod, requestUri, domainId, domainKey)
    }

    try{
      val status = (new HttpClient()).executeMethod(getMethod)
      status match {
        case HttpStatus.SC_OK => {
          val fileChannel = new FileOutputStream(file).getChannel
          val inChannel = Channels.newChannel(new BufferedInputStream(getMethod.getResponseBodyAsStream))
          try{
            fileChannel.transferFrom(inChannel, 0, getMethod.getResponseContentLength)
          }finally{
            fileChannel.close()
            inChannel.close()
          }
        }
        case _ => throw new StorageException("Failed to get resource data, code " + status)
      }
    }finally{
      getMethod.releaseConnection()
    }

    file
  }

  def addAuthHeader(method:HttpMethodBase, resource:String, domainId:String, domainKey:String) {

    val dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z")

    val dateString = dateFormat.format(new Date())

    val hashBase = resource + dateString + domainId

    val hash = HashUtil.hmac(domainKey, hashBase)

    val dateHeader = new Header("x-c3-date", dateString)
    method.addRequestHeader(dateHeader)

    val hostHeader = new Header("x-c3-sign", hash)
    method.addRequestHeader(hostHeader)
  }

  override def copy:DataStream = new RemoteSystemDataStream(host, secure, address, version, domainId, domainKey)

  override def finalize(){
    if(created){
      try{
        file.delete
        RemoteSystemDataWrapper.log.debug("Deleted tmp file for ra " + address)
      }catch{
        case e: Throwable => e.printStackTrace()
      }
    }
  }
}

object RemoteSystemDataWrapper{

  val log = Logger(getClass)

}