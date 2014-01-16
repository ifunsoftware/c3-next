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
package org.aphreet.c3.platform.remote.replication.impl.config

import collection.mutable
import org.aphreet.c3.platform.auth.AuthenticationManager
import org.aphreet.c3.platform.common.Logger
import org.aphreet.c3.platform.remote.replication._
import org.aphreet.c3.platform.remote.replication.impl.ReplicationPortRetriever
import org.aphreet.c3.platform.remote.replication.impl.data.encryption.{DataEncryptor, SymmetricKeyGenerator, AsymmetricDataEncryptor}
import org.aphreet.c3.platform.resource.IdGenerator
import scala.None
import akka.actor.Actor
import org.aphreet.c3.platform.remote.replication.impl.config.ReplicationNegotiator._

class ReplicationNegotiatorServer(val authManager: AuthenticationManager,
                            val configurationManager: ConfigurationManager,
                            val replicationManager: ReplicationManager,
                            val replicationPortRetriever: ReplicationPortRetriever) extends Actor{

  val log = Logger(getClass)

  val sharedKeys = new mutable.HashMap[String, String]

  {
    log info "Starting replication negotiator..."
  }

  def receive = {
    case NegotiateKeyExchangeMsg(systemId, publicKey) => {
      try{
        log info "Received negotiation request from: " + systemId

        val sharedKey = SymmetricKeyGenerator.generateAESKey

        log info "Generated shared key"

        sharedKeys.put(systemId, sharedKey)

        val encryptedSharedKey = AsymmetricDataEncryptor.encrypt(sharedKey.getBytes("UTF-8"), publicKey)

        log info "Encrypted shared key"

        sender ! NegotiateKeyExchangeMsgReply(STATUS_OK, encryptedSharedKey)

      }catch{
        case e: Throwable => {
          log.warn("Failed to esablish replication", e)
          sender ! NegotiateKeyExchangeMsgReply(STATUS_ERROR, null)
        }
      }
    }

    case NegotiateRegisterSourceMsg(systemId, configuration, login, password) => {

      try{
        val sharedKey = sharedKeys.get(systemId) match {
          case Some(x) => x
          case None => null
        }

        if(sharedKey == null){
          log warn "Failed to find shared key for system: " + systemId

          sender ! NegotiateRegisterSourceMsgReply(STATUS_ERROR, null)

        }else{

          val dataEncryptor = new DataEncryptor(sharedKey)

          val decodedConfiguration = dataEncryptor.decryptString(configuration)

          val decodedLogin = dataEncryptor.decryptString(login)

          val decodedPassword = dataEncryptor.decryptString(password)

          if(authManager.auth(decodedLogin, decodedPassword) != null){

            log info "Auth success. Applying remote configuration"

            val platformInfo = configurationManager.deserializeConfiguration(decodedConfiguration)

            configurationManager.processRemoteConfiguration(platformInfo)

            val host = platformInfo.host

            host.encryptionKey = sharedKey

            val replicationKey = IdGenerator.generateId(5)

            host.key = replicationKey

            replicationManager.registerReplicationSource(platformInfo.host)

            val localConfiguration = configurationManager.getLocalConfiguration
            localConfiguration.host.key = replicationKey

            val encryptedConfiguration = dataEncryptor.encrypt(configurationManager.serializeConfiguration(localConfiguration))

            log info "Successefully applied remote configuration"

            sender ! NegotiateRegisterSourceMsgReply(STATUS_OK, encryptedConfiguration)

          }else{
            log warn "Failed to register replication source, login failed"
            sharedKeys.remove(systemId)
            sender ! NegotiateRegisterSourceMsgReply(STATUS_ERROR, null)
          }

        }
      }catch{
        case e: Throwable => {
          log.warn("Failed to register replication source", e)
          sender ! NegotiateRegisterSourceMsgReply(STATUS_ERROR, null)
        }
      }
    }
  }
}
