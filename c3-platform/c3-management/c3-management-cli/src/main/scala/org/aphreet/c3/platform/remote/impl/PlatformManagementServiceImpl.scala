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

package org.aphreet.c3.platform.remote.impl

import org.aphreet.c3.platform.auth.AuthenticationManager
import org.aphreet.c3.platform.backup.BackupManager
import org.aphreet.c3.platform.common.Logger
import org.aphreet.c3.platform.domain.DomainManager
import org.aphreet.c3.platform.filesystem.FSManager
import org.aphreet.c3.platform.management.PlatformManagementEndpoint
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService
import org.aphreet.c3.platform.remote.replication.ReplicationManager
import org.aphreet.c3.platform.search.api.SearchManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component("platformManagementService")
class PlatformManagementServiceImpl extends PlatformManagementService {

  val logger = Logger(classOf[PlatformManagementServiceImpl])

  @Autowired
  var managementEndpoint: PlatformManagementEndpoint = _

  @Autowired
  var authenticationManager: AuthenticationManager = _

  @Autowired
  var replicationManager: ReplicationManager = _

  @Autowired
  var domainManager: DomainManager = _

  @Autowired
  var filesystemManager: FSManager = _

  @Autowired
  var backupManager: BackupManager = _

  @Autowired
  var searchManager: SearchManager = _


  def coreManagement = managementEndpoint

  def userManagement = authenticationManager

  def searchManagement = searchManager

  def fsManagement = filesystemManager

  def domainManagement = domainManager

  def backupManagement = backupManager

  def replicationManagement = replicationManager

}
