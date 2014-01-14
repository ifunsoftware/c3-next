package org.aphreet.c3.platform.search.lucene

import org.aphreet.c3.platform.access.{AccessManager, AccessMediator, AccessComponent}
import org.aphreet.c3.platform.common.{DefaultComponentLifecycle, C3AppHandle, C3Activator}
import org.aphreet.c3.platform.config.{PlatformConfigManager, ConfigPersister, PlatformConfigComponent}
import org.aphreet.c3.platform.statistics.{StatisticsManager, StatisticsComponent}
import org.aphreet.c3.platform.storage.{StorageManager, StorageComponent}
import org.aphreet.c3.platform.task.{TaskManager, TaskComponent}
import org.osgi.framework.BundleContext
import org.aphreet.c3.platform.search.lucene.impl.SearchComponentImpl
import org.aphreet.c3.platform.search.api.SearchManager
import org.aphreet.c3.platform.actor.ActorComponent
import akka.actor.ActorRefFactory

/**
 * Author: Mikhail Malygin
 * Date:   12/21/13
 * Time:   4:20 PM
 */
class C3SearchActivator extends C3Activator {

  def name: String = "c3-search-lucene"

  def createApplication(context: BundleContext, actorRefFactory: ActorRefFactory): C3AppHandle = {

    trait DependencyProvider extends  AccessComponent
    with StorageComponent
    with PlatformConfigComponent
    with TaskComponent
    with ActorComponent
    with StatisticsComponent {
      val accessManager = getService(context, classOf[AccessManager])

      val accessMediator = getService(context, classOf[AccessMediator])

      val statisticsManager = getService(context, classOf[StatisticsManager])

      val storageManager = getService(context, classOf[StorageManager])

      val taskManager = getService(context, classOf[TaskManager])

      val platformConfigManager = getService(context, classOf[PlatformConfigManager])

      val configPersister = getService(context, classOf[ConfigPersister])

      val actorSystem = actorRefFactory
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
