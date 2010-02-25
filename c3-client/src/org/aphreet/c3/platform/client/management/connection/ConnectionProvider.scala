package org.aphreet.c3.platform.client.management.connection

import org.aphreet.c3.platform.remote.api.management.PlatformManagementService
import org.aphreet.c3.platform.remote.api.access.PlatformAccessService

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Feb 23, 2010
 * Time: 1:51:30 AM
 * To change this template use File | Settings | File Templates.
 */

trait ConnectionProvider{

  def management:PlatformManagementService

  def access:PlatformAccessService

}