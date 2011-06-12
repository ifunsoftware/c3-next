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

import org.aphreet.c3.platform.common.WatchedActor
import org.aphreet.c3.platform.remote.replication._
import impl.data.encryption.{DataEncryptor, SymmetricKeyGenerator, AsymmetricDataEncryptor}
import scala.actors.remote.RemoteActor._
import org.apache.commons.logging.LogFactory
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.common.msg.DestroyMsg
import collection.mutable.HashMap
import org.aphreet.c3.platform.auth.AuthenticationManager
import org.springframework.beans.factory.annotation.Autowired


class ReplicationNegotiator extends WatchedActor{

  val log = LogFactory getLog getClass

  val sharedKeys = new HashMap[String, Array[Byte]]

  var authManager:AuthenticationManager = null

  var configurationManager:ConfigurationManager = null

  var replicationManager:ReplicationManager = null

  @Autowired
  def setAuthManager(manager:AuthenticationManager) = {authManager = manager}

  @Autowired
  def setConfigurationManager:ConfigurationManager = {configurationManager = manager}

  @Autowired
  def setReplicationManager:ReplicationManager = {replicationManager = manager}

  @PostConstruct
  def init{
    log info "Starting replication negotiator"

    this.start
  }

  @PreDestroy
  def destroy{
    log info "Stopping replication negotiator"

    this ! DestroyMsg
  }

  override def act{

    alive(7375)
    register('ReplicationNegotiator, this)

    loop{
      react{
        case DestroyMsg => this.exit

        case NegotiateKeyExchangeMsg(systemId, publicKey) => {

          log info "Received negotiation request from: " + systemId

          val sharedKey = SymmetricKeyGenerator.generateAESKey

          log info "Generated shared key"

          sharedKeys.put(systemId, sharedKey)

          val encryptedSharedKey = AsymmetricDataEncryptor.encrypt(sharedKey, publicKey)

          log info "Encrypted shared key"

          reply{
            NegotiateKeyExchangeMsgReply(encryptedSharedKey)
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
              reply{
                NegotiateRegisterSourceMsgReply("ERROR", null)
              }
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

                replicationManager.registerReplicationSource(platformInfo.host)

                val localConfiguration = configurationManager.getSerializedConfiguration

                val encryptedConfiguration = dataEncryptor.encrypt(localConfiguration)

                log info "Successefully applied remote configuration"

                reply{
                  NegotiateRegisterSourceMsgReply("OK", encryptedConfiguration)
                }

              }else{
                log warn "Failed to register replication source, login failed"
                reply{
                  NegotiateRegisterSourceMsgReply("ERROR", null)
                }
              }
              
            }
          }catch{
            case e => {
              log.warn("Failed to register replication source", e)
              reply{
                NegotiateRegisterSourceMsgReply("ERROR", null)
              }
            }
          }
        }
      }
    }
  }
}