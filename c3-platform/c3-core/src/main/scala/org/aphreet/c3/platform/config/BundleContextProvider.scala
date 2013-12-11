package org.aphreet.c3.platform.config

import org.osgi.framework.BundleContext

/**
 * Author: Mikhail Malygin
 * Date:   12/11/13
 * Time:   2:22 PM
 */
trait BundleContextProvider {

  def bundleContext: BundleContext

}
