package org.aphreet.c3.platform.remote.replication.impl

import org.aphreet.c3.platform.config.PlatformConfigManager

class NetworkReplicationSettingsRetriever(val platformConfigManager: PlatformConfigManager) {

  def localReplicationHost: String = {
    platformConfigManager.getPlatformProperties.getOrElse(ReplicationConstants.REPLICATION_NAT_HOST, NetworkSettings.replicationBindAddress)
  }

  def localReplicationPort: String = {
    platformConfigManager.getPlatformProperties.getOrElse(ReplicationConstants.REPLICATION_NAT_PORT, NetworkSettings.replicationBindPort)
  }
}
