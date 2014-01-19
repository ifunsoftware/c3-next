package org.aphreet.c3.platform.search.es

import akka.actor.ActorRefFactory
import org.aphreet.c3.platform.access.{AccessMediator, AccessManager, AccessComponent}
import org.aphreet.c3.platform.actor.ActorComponent
import org.aphreet.c3.platform.common.{C3ActorActivator, DefaultComponentLifecycle, C3AppHandle}
import org.aphreet.c3.platform.config.{ConfigPersister, PlatformConfigManager, PlatformConfigComponent}
import org.aphreet.c3.platform.search.api.SearchManager
import org.osgi.framework.BundleContext

/**
 * Author: Mikhail Malygin
 * Date:   1/13/14
 * Time:   5:09 PM
 */
class C3SearchActivator extends C3ActorActivator {

  def name: String = "c3-search-es"

  def createApplication(context: BundleContext, actorRefFactory: ActorRefFactory): C3AppHandle = {

    trait DependencyProvider extends AccessComponent
    with ActorComponent
    with PlatformConfigComponent {
      val accessManager = getService(context, classOf[AccessManager])

      val accessMediator = getService(context, classOf[AccessMediator])

      val platformConfigManager = getService(context, classOf[PlatformConfigManager])

      val configPersister = getService(context, classOf[ConfigPersister])

      val actorSystem = actorRefFactory
    }

    val module = new Object with DefaultComponentLifecycle
      with DependencyProvider
      with SearchComponentImpl

    new C3AppHandle {
      def registerServices(context: BundleContext) {
        registerService(context, classOf[SearchManager], module.searchManager)
      }

      val app = module
    }
  }
}

