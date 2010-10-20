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

package org.aphreet.c3.platform.remote.replication.impl

import actors.Actor
import org.aphreet.c3.platform.remote.api.management.ReplicationHost
import org.apache.commons.logging.LogFactory
import actors.remote.{RemoteActor, Node}
import org.aphreet.c3.platform.resource.Resource
import com.twmacinta.util.MD5
import org.aphreet.c3.platform.remote.replication.{ReplicateDelete, ReplicateUpdate, ReplicateAdd}

class ReplicationLink(val host:ReplicationHost) extends Actor{

  val log = LogFactory getLog getClass

  val remoteActor = {
    log info "Establishing replication link to " + host.systemId

    val port = ReplicationConstants.REPLICATION_PORT

    val peer = Node(host.hostname, port)

    val sink = RemoteActor.select(peer, 'ReplicationActor)

    link(sink)

    sink
  }

  override def act{
    loop{
      react{
        case _ => //do nothing here
      }
    }
  }

  def replicateAdd(resource:Resource) = {

    val bytes = resource.toByteArray

    val signature = getSignature(bytes)

    remoteActor ! ReplicateAdd(bytes, signature)

  }

  def replicateUpdate(resource:Resource) = {
    val bytes = resource.toByteArray

    val signature = getSignature(bytes)

    remoteActor ! ReplicateUpdate(bytes, signature)
  }

  def replicateDelete(address:String) = {

    val bytes = address.getBytes("UTF-8")

    val signature = getSignature(bytes)

    remoteActor ! ReplicateDelete(address, signature)
  }

  def getSignature(data:Array[Byte]):String = {

    val md5 = new MD5
    md5.Update(data)
    md5.Update(host.systemId)
    md5.Update(host.key)
    md5.Final

    md5.asHex
  }


  def close{
    log info "Closing replication link"
    unlink(remoteActor)
  }
}