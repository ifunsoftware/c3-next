package org.aphreet.c3.platform.client.management.connection.impl

import org.aphreet.c3.platform.client.management.connection.ConnectionProvider
import org.aphreet.c3.platform.client.common.SpringWsAccessor
import org.aphreet.c3.platform.remote.api.access.PlatformAccessService
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService
import org.aphreet.c3.platform.remote.api.ws.{PlatformWSManagementEndpoint, PlatformWSAccessEndpoint}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 23, 2010
 * Time: 2:19:38 AM
 * To change this template use File | Settings | File Templates.
 */

class WSConnectionProvider extends ConnectionProvider with SpringWsAccessor{

  def management:PlatformManagementService =
    obtainWebService("http://localhost:8080", "ManagementService", "remote.c3.aphreet.org",
                                      "PlatformWSManagementEndpointImplPort", classOf[PlatformWSManagementEndpoint])




  def access:PlatformAccessService =
    obtainWebService("http://localhost:8080", "AccessService", "remote.c3.aphreet.org",
                                  "PlatformWSAccessEndpointImplPort", classOf[PlatformWSAccessEndpoint])
}