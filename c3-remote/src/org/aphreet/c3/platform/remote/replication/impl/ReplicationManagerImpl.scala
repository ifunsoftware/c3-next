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

import org.aphreet.c3.platform.remote.replication.ReplicationManager
import org.springframework.stereotype.Component
import org.aphreet.c3.platform.storage.StorageManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.remoting.jaxws.JaxWsPortProxyFactoryBean
import java.net.URL
import org.aphreet.c3.platform.exception.ConfigurationException
import collection.mutable.{HashSet, HashMap}
import org.apache.commons.logging.LogFactory
import org.springframework.context.annotation.Scope
import org.aphreet.c3.platform.config.PlatformConfigManager
import org.aphreet.c3.platform.common.Constants
import org.aphreet.c3.platform.remote.api.management.{ReplicationHost, StorageDescription, PlatformManagementService}
import javax.annotation.{PreDestroy, PostConstruct}
import org.aphreet.c3.platform.access.{AccessManager, ResourceDeletedMsg, ResourceUpdatedMsg, ResourceAddedMsg}
import org.aphreet.c3.platform.common.msg.{UnregisterListenerMsg, RegisterListenerMsg, DestroyMsg}
import actors.AbstractActor
import actors.remote.{RemoteActor, Node}

@Component("replicationManager")
@Scope("singleton")
class ReplicationManagerImpl extends ReplicationManager{

  val log = LogFactory getLog getClass

  var localReplicationActor:ReplicationActor = null

  var platformConfigManager:PlatformConfigManager = null

  var storageManager:StorageManager = null

  var sourcesConfigAccessor:ReplicationSourcesConfigAccessor = null

  var targetsConfigAccessor:ReplicationTargetsConfigAccessor = null

  var accessManager:AccessManager = null


  var systemId = ""

  var currentTargetConfig:Map[String, ReplicationHost] = Map()

  var currentSourceConfig:Map[String, ReplicationHost] = Map()

  @Autowired
  def setStorageManager(manager:StorageManager) = {storageManager = manager}

  @Autowired
  def setSourcesConfigAccessor(accessor:ReplicationSourcesConfigAccessor) = {sourcesConfigAccessor = accessor}

  @Autowired
  def setTargetsConfigAccessor(accessor:ReplicationTargetsConfigAccessor) = {targetsConfigAccessor = accessor}

  @Autowired
  def setConfigManager(manager:PlatformConfigManager) = {platformConfigManager = manager}

  @Autowired
  def setLocalReplicationActor(actor:ReplicationActor) = {localReplicationActor = actor}

  @Autowired
  def setAccessManager(manager:AccessManager) = {accessManager = manager}


  var remoteReplicationActors = Map[String, ReplicationLink]()


  @PostConstruct
  def init{

    log info "Starting replication manager..."

    platformConfigManager.getPlatformProperties.get(Constants.C3_SYSTEM_ID) match {
      case Some(value) => systemId = value
      case None => throw new ConfigurationException("Failed to get current system id")
    }


    currentSourceConfig = sourcesConfigAccessor.load
    localReplicationActor.startWithConfig(currentSourceConfig)

    currentTargetConfig = targetsConfigAccessor.load

    for((id, host) <- currentTargetConfig) {
      remoteReplicationActors = remoteReplicationActors + ((id, new ReplicationLink(host)))
    }


    log info "Registering accessManager listener..."

    accessManager ! RegisterListenerMsg(this)

    log info "Replicationg manager started"
  }

  @PreDestroy
  def destroy{
    log info "Destroying ReplicationManager"

    accessManager ! UnregisterListenerMsg(this)

    this ! DestroyMsg
  }

  override def act{
    loop{
      react{
        case ResourceAddedMsg(resource) => {
          try{
            for((id, link) <- remoteReplicationActors){
              link.replicateAdd(resource)
            }
          }catch{
            case e => log.error("Failed to replicate resource", e)
          }

        }
        case ResourceUpdatedMsg(resource) => {
          try{
            for((id, link) <- remoteReplicationActors){
              link.replicateUpdate(resource)
            }
          }catch{
            case e => log.error("Failed to replicate resource", e)
          }
        }
        case ResourceDeletedMsg(address) => {
          try{
            for((id, link) <- remoteReplicationActors){
              link.replicateDelete(address)
            }
          }catch{
            case e => log.error("Failed to replicate resource", e)
          }
        }
        case DestroyMsg => {
          this.exit
          for((id, link) <- remoteReplicationActors){
            link.close
          }

          remoteReplicationActors = Map()
        }
      }
    }
  }

  /**
   * First check that all storage ids from this machine exist on remote machine
   *
   * After that, generate replication key
   *
   * And write remote source config and local target
   */
  def establishReplication(host:String, user:String, password:String) = {

    val localHostName = platformConfigManager.getPlatformProperties.get(Constants.C3_PUBLIC_HOSTNAME) match{
      case Some(x) => x
      case None => throw new ConfigurationException("Can't esablish replication until public hostname is not set")
    }
    
    log info "Connecting to remote system..."

    val managementService = getManagementService(host, user, password)

    log info "Done"

    val options = managementService.platformProperties.filter(_.key == Constants.C3_SYSTEM_ID)

    if(options.isEmpty){
      throw new ConfigurationException("Failed to get remote system id")
    }

    val remoteSystemId = options.head.value

    val remoteStorages = managementService.listStorages.toList
    val localStorages = storageManager.listStorages

    log info "Comparing storage configuration..."

    val additionalIds = new StorageSynchronizer().getAdditionalIds(remoteStorages, localStorages)

    log info "Done"

    log info "Applying configuration changes..."

    for((id, secId) <- additionalIds){
      managementService.addStorageSecondaryId(id, secId)
    }

    log info "Done"
    
    //TODO change this in future
    val key = System.currentTimeMillis.toString

    managementService.registerReplicationSource(new ReplicationHost(systemId, localHostName, key))

    registerReplicationTarget(new ReplicationHost(remoteSystemId, host, key))
  }

  def cancelReplication(id:String) = {
    targetsConfigAccessor.update(config => config - id)
    currentTargetConfig = targetsConfigAccessor.load

    val link = remoteReplicationActors.get(id).get

    remoteReplicationActors = remoteReplicationActors - id

    link.close
  }

  def registerReplicationSource(host:ReplicationHost) = {
    sourcesConfigAccessor.update(config => config + ((host.systemId, host)))

    currentSourceConfig = sourcesConfigAccessor.load
    localReplicationActor.updateConfig(currentSourceConfig)

    log info "Registered replication source " + host.hostname + " with id " + host.systemId
  }

  private def registerReplicationTarget(host:ReplicationHost) = {
    targetsConfigAccessor.update(config => config + ((host.systemId, host)))
    currentTargetConfig = targetsConfigAccessor.load

    remoteReplicationActors = remoteReplicationActors + ((host.systemId, new ReplicationLink(host)))

    log info "Registered replication target " + host.hostname + " with id " + host.systemId
  }

  private def getManagementService(host:String, user:String, password:String):PlatformManagementService = {
    try {
      val factory: JaxWsPortProxyFactoryBean = new JaxWsPortProxyFactoryBean
      factory.setServiceInterface(classOf[PlatformManagementService])
      factory.setWsdlDocumentUrl(new URL("http://" + host + ":" + ReplicationConstants.HTTP_PORT + "/c3-remote/ws/management?WSDL"))
      factory.setNamespaceUri("remote.c3.aphreet.org")
      factory.setServiceName("ManagementService")
      factory.setUsername(user)
      factory.setPassword(password)
      factory.setPortName("PlatformManagementServiceImplPort")
      factory.setMaintainSession(true)
      factory.afterPropertiesSet

      factory.getObject.asInstanceOf[PlatformManagementService]
    } catch {
      case e: javax.xml.ws.WebServiceException => {
        throw new ConfigurationException("Failed to connect to remote host" + e.getMessage)
      }
    }
  }

}