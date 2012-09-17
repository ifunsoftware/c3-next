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

import org.aphreet.c3.platform.remote.api.management.ReplicationHost
import com.twmacinta.util.MD5
import org.aphreet.c3.platform.remote.replication.ReplicationSignature

class ReplicationSignatureCalculator(val localSystemId:String, val host:ReplicationHost){

  def calculate(bytes:Array[Byte]):ReplicationSignature = {

    val md5 = new MD5
    md5.Update(bytes)
    md5.Update(localSystemId)
    md5.Update(host.key)
    md5.Final


    ReplicationSignature(localSystemId, md5.asHex)
  }

  def calculate(string:String):ReplicationSignature =
    calculate(string.getBytes("UTF-8"))

  def verify(bytes:Array[Byte], signature:ReplicationSignature):Boolean = {

    if(host.systemId != signature.systemId) return false

    val md5 = new MD5
    md5.Update(bytes)
    md5.Update(host.systemId)
    md5.Update(host.key)
    md5.Final

    md5.asHex == signature.hash

  }

  def verify(string:String, signature:ReplicationSignature):Boolean =
    verify(string.getBytes("UTF-8"), signature)
}

object ReplicationSignatureCalculator{

  def foundAndVerify(bytes:Array[Byte], signature:ReplicationSignature, hosts:Map[String, ReplicationHost]):Option[ReplicationHost] = {

    hosts.get(signature.systemId) match {
      case Some(host) =>
        if(new ReplicationSignatureCalculator(null, host).verify(bytes, signature)){
          Some(host)
        }else{
          None
        }
      case None => None
    }

  }

  def foundAndVerify(data:String, signature:ReplicationSignature, hosts:Map[String, ReplicationHost]):Option[ReplicationHost] = {

    hosts.get(signature.systemId) match {
      case Some(host) =>
        if(new ReplicationSignatureCalculator(null, host).verify(data, signature)){
          Some(host)
        }else{
          None
        }
      case None => None
    }

  }
}
