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
package org.aphreet.c3.platform.client.management.connection.impl

import org.aphreet.c3.platform.client.management.connection.ConnectionProvider
import org.aphreet.c3.platform.remote.api.access.PlatformAccessService
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService
import org.aphreet.c3.platform.remote.api.ws.{RemoteApiEndpoint, PlatformWSManagementEndpoint, PlatformWSAccessEndpoint}
import org.aphreet.c3.platform.client.common.{VersionUtils, SpringWsAccessor}

class WSConnectionProvider(val host:String, val user:String, val password:String) extends ConnectionProvider with SpringWsAccessor{

  {
    val versionService = obtainWebService("http://" + host, user, password, "RemoteApiService", "remote.c3.aphreet.org",
                                      "RemoteApiEndpointImplPort", classOf[RemoteApiEndpoint])

    val serviceApiVersion = versionService.getVersion

    println("Connected. Server api version " + serviceApiVersion)

    val clientApiVersion = VersionUtils.clientVersion

    if(serviceApiVersion != clientApiVersion){
      println("Warning: client and server are using different api verison. Some commands may not work. Please use the same version of client and server.")
    }
  }

  def management:PlatformManagementService =
    obtainWebService("http://" + host, user, password, "ManagementService", "remote.c3.aphreet.org",
                                      "PlatformWSManagementEndpointImplPort", classOf[PlatformWSManagementEndpoint])




  def access:PlatformAccessService =
    obtainWebService("http://" + host, user, password, "AccessService", "remote.c3.aphreet.org",
                                  "PlatformWSAccessEndpointImplPort", classOf[PlatformWSAccessEndpoint])


}