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

package org.aphreet.c3.platform.client.access.http

import java.text.SimpleDateFormat
import org.apache.commons.httpclient.{Header, HttpMethodBase}
import java.util.Date
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import org.aphreet.c3.platform.client.common.HashUtil

abstract class AbstractHttpAccessor(val domain:String, val secret:String){

  def addAuthHeader(method:HttpMethodBase, resource:String) = {
    if(domain != "anonymous"){

      val dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z")

      val dateString = dateFormat.format(new Date())

      val hashBase = resource + dateString + domain

      val hash = HashUtil.hmac(secret, hashBase)

      val header = new Header("x-c3-sign", hash)
      method.addRequestHeader(header)

      val domainHeader = new Header("x-c3-domain", domain)
      method.addRequestHeader(domainHeader)

      val dateHeader = new Header("x-c3-date", dateString)
      method.addRequestHeader(dateHeader)
    }
  }
}