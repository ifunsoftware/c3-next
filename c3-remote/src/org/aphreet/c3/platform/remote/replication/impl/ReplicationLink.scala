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

import org.aphreet.c3.platform.remote.api.management.ReplicationHost
import org.apache.commons.logging.LogFactory
import actors.remote.{RemoteActor, Node}
import org.aphreet.c3.platform.resource.Resource
import com.twmacinta.util.MD5
import actors.{AbstractActor, Actor}
import org.aphreet.c3.platform.common.msg.DestroyMsg
import org.aphreet.c3.platform.access.{ResourceUpdatedMsg, ResourceDeletedMsg, ResourceAddedMsg}
import org.aphreet.c3.platform.remote.replication._

class ReplicationLink(val host:ReplicationHost) extends Actor{

  val log = LogFactory getLog getClass

  def isStarted:Boolean = started

  private var started = false


  override def act{

    val calculator = new ReplicationSignatureCalculator(host)

    log info "Establishing replication link to " + host.systemId

    val port = ReplicationConstants.REPLICATION_PORT

    val peer = Node(host.hostname, port)

    val remoteActor = RemoteActor.select(peer, 'ReplicationActor)

    link(remoteActor)

    started = true

    loop{
      react{

        case ResourceAddedMsg(resource) => {
          val bytes = resource.toByteArray

          remoteActor ! ReplicateAddMsg(bytes, calculator.calculate(bytes))
        }

        case ReplicateAddAckMsg(address, signature) => {

        }



        case ResourceUpdatedMsg(resource) => {
          val bytes = resource.toByteArray

          remoteActor ! ReplicateUpdateMsg(bytes, calculator.calculate(bytes))
        }

        case ReplicateUpdateAckMsg(address, signature) => {

        }



        case ResourceDeletedMsg(address) => {
          remoteActor ! ReplicateDeleteMsg(address, calculator.calculate(address))
        }

        case ReplicateDeleteAckMsg(address, signature) => {
          if(calculator.verify(address, signature)){

          }
        }


        case DestroyMsg => {
          log info "Destroying replication link to " + host.toString 
          unlink(remoteActor)
          this.exit
        }
      }
    }
  }

  def close{
    log info "Closing replication link"
    this ! DestroyMsg
  }
}