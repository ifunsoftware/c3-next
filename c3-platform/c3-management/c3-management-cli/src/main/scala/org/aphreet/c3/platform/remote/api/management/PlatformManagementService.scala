package org.aphreet.c3.platform.remote.api.management

import org.aphreet.c3.platform.auth.AuthenticationManager
import org.aphreet.c3.platform.backup.BackupManager
import org.aphreet.c3.platform.domain.DomainManager
import org.aphreet.c3.platform.filesystem.FSManager
import org.aphreet.c3.platform.management.PlatformManagementEndpoint
import org.aphreet.c3.platform.remote.replication.ReplicationManager
import org.aphreet.c3.platform.search.api.SearchManager

trait PlatformManagementService {

  def coreManagement: PlatformManagementEndpoint

  def userManagement: AuthenticationManager

  def searchManagement: SearchManager

  def fsManagement: FSManager

  def domainManagement: DomainManager

  def backupManagement: BackupManager

  def replicationManagement: ReplicationManager

}