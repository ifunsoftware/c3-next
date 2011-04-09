/**
 * Copyright (c) 2011, Mikhail Malygin
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

package org.aphreet.c3.platform.client.access.tools

import org.apache.commons.httpclient.methods.GetMethod
import java.text.SimpleDateFormat
import java.util.Date
import org.apache.commons.httpclient.{HttpStatus, HttpClient, Header, HttpMethodBase}
import org.aphreet.c3.platform.client.common.{HashUtil, CLI}

class ReplicationReader(override val args: Array[String]) extends CLI(args) {

  def clientName = "Writer"

  def cliDescription = parameters(
    HOST_ARG,
    ID_ARG,
    ADDRESS_ARG,
    KEY_ARG,
    HELP_ARG
  )

  def run {
    val host:String = HOST_ARG
    val id:String = ID_ARG
    val address:String = ADDRESS_ARG
    val key:String = KEY_ARG

    val requestUri = "/rest/data/replication/" + address + "/1"

    val getMethod = new GetMethod(host + requestUri)

    addAuthHeader(getMethod, requestUri, id, key)

    val client = new HttpClient

    try{
      val status = client.executeMethod(getMethod)

      status match {
        case HttpStatus.SC_OK => {
          println("OK: \n" + getMethod.getResponseBodyAsString)
        }
        case _ => {
          println("Error: \n" + getMethod.getResponseBodyAsString)
        }
      }
    }finally{
      getMethod.releaseConnection
    }
  }

  def addAuthHeader(method:HttpMethodBase, resource:String, localId:String, secret:String) = {

    val dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z")

    val dateString = dateFormat.format(new Date())

    val hashBase = resource + localId + dateString

    val hash = HashUtil.hmac(secret, hashBase)

    val header = new Header("x-c3-repl", hash)
    method.addRequestHeader(header)

    val dateHeader = new Header("x-c3-date", dateString)
    method.addRequestHeader(dateHeader)

    val hostHeader = new Header("x-c3-host", localId)
    method.addRequestHeader(hostHeader)
  }




}