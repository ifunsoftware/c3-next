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

package org.aphreet.c3.platform.access.impl

import javax.annotation.{PreDestroy, PostConstruct}
import org.apache.commons.logging.LogFactory
import actors.Actor
import org.aphreet.c3.platform.common.msg._
import org.aphreet.c3.platform.access.{ResourceDeletedMsg, ResourceUpdatedMsg, ResourceAddedMsg, AccessMediator}
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Scope
import collection.mutable.HashMap
import org.springframework.beans.factory.annotation.Qualifier

@Component("accessMediator")
@Scope("singleton")
@Qualifier("AccessMediator")
class AccessMediatorImpl extends AccessMediator {

  val log = LogFactory getLog getClass

  var accessListeners = new HashMap[Actor, Symbol]

  @PostConstruct
  def init() {
    log info "Starting access mediator"
    this.start()
  }

  override def act() {
    loop{
      react{
        case DestroyMsg => {
          log info "AccessMediator stopped"
          this.exit()
        }

        case RegisterNamedListenerMsg(actor, name) =>
          log debug "Registering listener " + actor.toString
          accessListeners.put(actor, name)
          log debug accessListeners.toString
        
        case UnregisterNamedListenerMsg(actor, name) =>
          log debug "Unregistering listener " + actor.toString
          accessListeners.remove(actor)
          log debug accessListeners.toString

        case ResourceAddedMsg(resource, source) => {

          accessListeners.foreach{e => {
            if(e._2 != source)
              e._1 ! ResourceAddedMsg(resource, source)
            }
          }
        }

        case ResourceUpdatedMsg(resource, source) => {
          accessListeners.foreach{e => {
            if(e._2 != source)
              e._1 ! ResourceUpdatedMsg(resource, source)
            }
          }
        }

        case ResourceDeletedMsg(address, source) => {
          accessListeners.foreach {e => {
            if(e._2 != source)
              e._1 ! ResourceDeletedMsg(address, source)
            }
          }
        }
      }
    }

  }

  @PreDestroy
  def destroy() {
    log info "Stopping access mediator..."
    this ! DestroyMsg
  }
}
