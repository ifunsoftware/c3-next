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

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.xml.DomDriver
import org.aphreet.c3.platform.common.{Logger, Constants}
import org.aphreet.c3.platform.config.PlatformConfigManager
import org.aphreet.c3.platform.domain.DomainManager
import org.aphreet.c3.platform.exception.ConfigurationException
import org.aphreet.c3.platform.filesystem.FSManager
import org.aphreet.c3.platform.remote.replication.ReplicationHost
import org.aphreet.c3.platform.remote.replication.impl.NetworkReplicationSettingsRetriever
import org.aphreet.c3.platform.remote.replication.impl.ReplicationConstants._

class ConfigurationManager(val fsManager: FSManager, val domainManager: DomainManager, val platformConfigManager: PlatformConfigManager) extends DtoConvertor {

  val networkSettingsRetriever = new NetworkReplicationSettingsRetriever(platformConfigManager)

  val log = Logger(getClass)

  def processSerializedRemoteConfiguration(configuration: String) {
    processRemoteConfiguration(deserializeConfiguration(configuration))
  }

  def deserializeConfiguration(configuration: String): PlatformInfo = {
    createStream().fromXML(configuration).asInstanceOf[PlatformInfo]
  }

  def getSerializedConfiguration: String = {
    serializeConfiguration(getLocalConfiguration)
  }

  def serializeConfiguration(platformInfo: PlatformInfo): String = {
    createStream().toXML(platformInfo)
  }

  private def createStream(): XStream = {
    val xStream = new XStream(new DomDriver("UTF-8"))
    xStream.setClassLoader(classOf[PlatformInfo].getClassLoader)
    xStream
  }

  def processRemoteConfiguration(info: PlatformInfo) {
    synchronized {

      log debug "Importing fs roots..."

      importFsRoots(info.fsRoots)

      log debug "Improting domains..."

      importDomains(info.domains, info.systemId)
    }
  }

  def getLocalConfiguration: PlatformInfo = {

    val domains = domainManager.domainList.map(d => new DomainDescription(d.id, d.name, d.key, d.mode.name, d.deleted)).toSeq.toArray

    val fsRoots = fsManager.fileSystemRoots.map(e => new Pair(e._1, e._2)).toSeq.toArray

    PlatformInfo(platformConfigManager.getSystemId,
      createLocalReplicationHost,
      domains,
      fsRoots)

  }

  def createLocalReplicationHost: ReplicationHost = {
    val propertyRetriever = createLocalPropertyRetriever

    val systemId = propertyRetriever(Constants.C3_SYSTEM_ID)
    val systemHost = networkSettingsRetriever.localReplicationHost
    val httpPort = propertyRetriever(HTTP_PORT_KEY).toInt
    val httpsPort = propertyRetriever(HTTPS_PORT_KEY).toInt
    val replicationPort = networkSettingsRetriever.localReplicationPort.toInt

    ReplicationHost(systemId, systemHost, null, httpPort, httpsPort, replicationPort, null)
  }

  private def createLocalPropertyRetriever: (String) => String = {
    (key: String) => platformConfigManager.getPlatformProperties.get(key) match {
      case Some(value) => value
      case None => throw new ConfigurationException("Failed to get property " + key)
    }
  }

  private def importDomains(remoteDomains: Array[DomainDescription], remoteSystemId: String) {
    for (domain <- remoteDomains) {
      domainManager.importDomain(domainFromDescription(domain), remoteSystemId)
    }
  }

  private def importFsRoots(remoteRoots: Array[Pair]) {
    for (pair <- remoteRoots) {
      fsManager.importFileSystemRoot(pair.key, pair.value)
    }
  }
}