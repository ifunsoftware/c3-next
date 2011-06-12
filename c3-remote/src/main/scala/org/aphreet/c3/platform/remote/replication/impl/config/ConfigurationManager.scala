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

import org.springframework.stereotype.Component
import org.aphreet.c3.platform.filesystem.FSManager
import org.springframework.beans.factory.annotation.Autowired
import org.aphreet.c3.platform.domain.DomainManager
import org.aphreet.c3.platform.storage.StorageManager
import org.aphreet.c3.platform.remote.impl.PlatformManagementServiceUtil
import org.aphreet.c3.platform.config.PlatformConfigManager
import com.thoughtworks.xstream.io.xml.DomDriver
import com.thoughtworks.xstream.XStream
import org.apache.commons.logging.LogFactory
import org.aphreet.c3.platform.remote.api.management.{ReplicationHost, StorageDescription, DomainDescription, Pair}
import org.aphreet.c3.platform.exception.ConfigurationException
import org.aphreet.c3.platform.common.Constants
import org.aphreet.c3.platform.remote.replication.impl.ReplicationConstants._

@Component
class ConfigurationManager{

  val log = LogFactory getLog getClass

  var fsManager:FSManager = _

  var domainManager:DomainManager = _

  var storageManager:StorageManager = _

  var platformConfigManager:PlatformConfigManager = _

  @Autowired
  def setFsManager(manager:FSManager) = {fsManager = manager}

  @Autowired
  def setDomainManager(manager:DomainManager) = {domainManager = manager}

  @Autowired
  def setStorageManager(manager:StorageManager) = {storageManager = manager}

  @Autowired
  def setPlatformConfigManager(manager:PlatformConfigManager) = {platformConfigManager = manager}


  def processSerializedRemoteConfiguration(configuration:String) = {
    processRemoteConfiguration(deserializeConfiguration(configuration))
  }

  def deserializeConfiguration(configuration:String):PlatformInfo = {
    val xStream = new XStream(new DomDriver("UTF-8"))
    xStream.setClassLoader(getClass.getClassLoader)

    xStream.fromXML(configuration).asInstanceOf[PlatformInfo]
  }

  def getSerializedConfiguration:String = {
    val xStream = new XStream(new DomDriver("UTF-8"))

    xStream.toXML(getLocalConfiguration)
  }
  
  def processRemoteConfiguration(info:PlatformInfo) = {
    synchronized{

      log debug "Importing new storage ids..."

      importStorages(info.storages)

      log debug "Importing fs roots..."

      importFsRoots(info.fsRoots)

      log debug "Improting domains..."

      importDomains(info.domains, info.systemId)
    }
  }

  def getLocalConfiguration:PlatformInfo = {

    val storageDescriptions = storageManager.listStorages
      .map(s => PlatformManagementServiceUtil.storageToDescription(s)).toSeq.toArray

    val domains = domainManager.domainList.map(d => new DomainDescription(d.id, d.name, d.key, d.mode.name)).toSeq.toArray

    val fsRoots = fsManager.fileSystemRoots.map(e => new Pair(e._1, e._2)).toSeq.toArray

    PlatformInfo(platformConfigManager.getSystemId,
      createLocalReplicationHost,
      storageDescriptions,
      domains,
      fsRoots)
    
  }

  def createLocalReplicationHost:ReplicationHost = {
    createReplicationHost(createLocalPropertyRetriever)
  }

  private def createReplicationHost(propertyRetriever:Function1[String, String]):ReplicationHost = {

    val systemId = propertyRetriever(Constants.C3_SYSTEM_ID)
    val systemHost = propertyRetriever(Constants.C3_PUBLIC_HOSTNAME)
    val httpPort = propertyRetriever(HTTP_PORT_KEY).toInt
    val httpsPort = propertyRetriever(HTTPS_PORT_KEY).toInt
    val replicationPort = propertyRetriever(REPLICATION_PORT_KEY).toInt
    val secretKey = secret

    ReplicationHost(systemId, systemHost, secretKey, httpPort, httpsPort, replicationPort, null)
  }

  private def createLocalPropertyRetriever:Function1[String, String] = {
    ((key:String) => platformConfigManager.getPlatformProperties.get(key) match {
      case Some(value) => value
      case None => throw new ConfigurationException("Failed to get property " + key)
    })
  }

  private def importDomains(remoteDomains:Array[DomainDescription], remoteSystemId:String) = {

    for(domain <- remoteDomains){
      domainManager.importDomain(PlatformManagementServiceUtil.domainFromDescription(domain), remoteSystemId)
    }

  }
  
  private def importFsRoots(remoteRoots:Array[Pair]) = {

    for(pair <- remoteRoots){
      fsManager.importFileSystemRoot(pair.key, pair.value)
    }
  }

  private def importStorages(remoteStorages:Array[StorageDescription]) = {
    val storageDescriptions = storageManager.listStorages
         .map(s => PlatformManagementServiceUtil.storageToDescription(s)).toSeq.toArray


    val synchronizer = new StorageSynchronizer

    val additionalStorageIds = synchronizer.compareStorageConfigs(remoteStorages.toList, storageDescriptions.toList)

    for((storageId, additional) <- additionalStorageIds){
      storageManager.addSecondaryId(storageId, additional)
    }
  }

}