package org.aphreet.c3.platform.client.management.connection.impl

import org.aphreet.c3.platform.client.management.connection.ConnectionProvider
import org.aphreet.c3.platform.client.common.SpringRmiAccessor
import org.aphreet.c3.platform.remote.api.access.PlatformAccessService
import org.aphreet.c3.platform.remote.api.management.PlatformManagementService
import org.aphreet.c3.platform.remote.api.rmi.access.PlatformRmiAccessService
import org.aphreet.c3.platform.remote.api.rmi.management.PlatformRmiManagementService

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 23, 2010
 * Time: 1:54:27 AM
 * To change this template use File | Settings | File Templates.
 */

class RmiConnectionProvider extends ConnectionProvider with SpringRmiAccessor{

  def management:PlatformManagementService =
    obtainRmiService("rmi://127.0.0.1:1299/PlatformRmiManagementEndPoint",
                                      classOf[PlatformRmiManagementService])


  

  def access:PlatformAccessService =
    obtainRmiService("rmi://127.0.0.1:1299/PlatformRmiAccessEndPoint",
                                  classOf[PlatformRmiAccessService])


}
