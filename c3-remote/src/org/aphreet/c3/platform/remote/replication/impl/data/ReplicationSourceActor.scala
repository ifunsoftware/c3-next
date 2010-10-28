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

import actors.Actor

import org.aphreet.c3.platform.access._
import org.aphreet.c3.platform.common.msg._
import org.aphreet.c3.platform.remote.replication._

import org.springframework.stereotype.Component
import org.springframework.context.annotation.Scope
import org.springframework.beans.factory.annotation.Autowired

import javax.annotation._

import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.remote.api.management.ReplicationHost


@Component
@Scope("singleton")
class ReplicationSourceActor extends Actor {

  private var remoteReplicationActors = Map[String, ReplicationLink]()

  val log = LogFactory getLog getClass

  var accessManager:AccessManager = null

  var manager:ReplicationManager = null

  @Autowired
  def setAccessManager(manager:AccessManager) = {accessManager = manager}

  def startWithConfig(config:Map[String, ReplicationHost], manager:ReplicationManager) = {

    log info "Starting ReplicationSourceActor..."

    this.manager = manager

    for((id, host) <- config) {
      remoteReplicationActors = remoteReplicationActors + ((id, new ReplicationLink(host)))
    }

    accessManager ! RegisterListenerMsg(this)

    this.start

    log info "ReplicationSourceActor started"
  }

  override def act{
    loop{
      react{
        case ResourceAddedMsg(resource) => {
          try{
            for((id, link) <- remoteReplicationActors){
              if(!link.isStarted) link.start
              link ! ResourceAddedMsg(resource)
            }
          }catch{
            case e => log.error("Failed to replicate resource", e)
          }

        }
        case ResourceUpdatedMsg(resource) => {
          try{
            for((id, link) <- remoteReplicationActors){
              if(!link.isStarted) link.start
              link ! ResourceUpdatedMsg(resource)
            }
          }catch{
            case e => log.error("Failed to replicate resource", e)
          }
        }
        case ResourceDeletedMsg(address) => {
          try{
            for((id, link) <- remoteReplicationActors){
              if(!link.isStarted) link.start
              link ! ResourceDeletedMsg(address)
            }
          }catch{
            case e => log.error("Failed to replicate resource", e)
          }
        }

        case QueuedTasks => {
          log debug "Getting list of queued resources"
          for((id, link) <- remoteReplicationActors){
            if(link.isStarted){
              link ! QueuedTasks
            }
          }
        }

        case QueuedTasksReply(entries) => {
          manager ! QueuedTasksReply(entries)
        }

        case DestroyMsg => {

          try{
            for((id, link) <- remoteReplicationActors){
              link.close
            }

            remoteReplicationActors = Map()

          }finally{
            log info "ReplicationSourceActor stopped"
            this.exit
          }
        }
      }
    }
  }

  def addReplicationTarget(host:ReplicationHost) = {
    remoteReplicationActors = remoteReplicationActors + ((host.systemId, new ReplicationLink(host)))
  }

  def removeReplicationTarget(remoteSystemId:String) = {
    val link = remoteReplicationActors.get(remoteSystemId).get

    remoteReplicationActors = remoteReplicationActors - remoteSystemId

    link.close
  }

  @PreDestroy
  def destroy{
    log info "Stopping ReplicationSourceActor..."
    this ! DestroyMsg
  }
}