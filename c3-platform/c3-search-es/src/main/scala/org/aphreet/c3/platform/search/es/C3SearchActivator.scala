package org.aphreet.c3.platform.search.es

import org.aphreet.c3.platform.common.{DefaultComponentLifecycle, C3AppHandle, C3Activator}
import org.osgi.framework.BundleContext
import org.aphreet.c3.platform.access.{AccessMediator, AccessManager, AccessComponent}
import org.aphreet.c3.platform.config.{ConfigPersister, PlatformConfigManager, PlatformConfigComponent}
import org.aphreet.c3.platform.search.api.SearchManager

/**
 * Author: Mikhail Malygin
 * Date:   1/13/14
 * Time:   5:09 PM
 */
class C3SearchActivator extends C3Activator {

  def name: String = "c3-search-es"

  def createApplication(context: BundleContext): C3AppHandle = {

    trait DependencyProvider extends  AccessComponent
    with PlatformConfigComponent {
      val accessManager = getService(context, classOf[AccessManager])

      val accessMediator = getService(context, classOf[AccessMediator])

      val platformConfigManager = getService(context, classOf[PlatformConfigManager])

      val configPersister = getService(context, classOf[ConfigPersister])
    }

    val module = new Object with DefaultComponentLifecycle
      with DependencyProvider
      with SearchComponentImpl

    new C3AppHandle {
      def registerServices(context: BundleContext){
        registerService(context, classOf[SearchManager], module.searchManager)
      }

      val app = module
    }
  }
}

