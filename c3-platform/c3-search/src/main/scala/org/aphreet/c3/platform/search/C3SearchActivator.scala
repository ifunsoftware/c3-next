package org.aphreet.c3.platform.search

import org.aphreet.c3.platform.common.{DefaultComponentLifecycle, C3AppHandle, C3Activator}
import org.osgi.framework.BundleContext
import org.aphreet.c3.platform.config.{PlatformConfigManager, ConfigPersister, PlatformConfigComponent}
import org.aphreet.c3.platform.storage.{StorageManager, StorageComponent}
import org.aphreet.c3.platform.access.{AccessManager, AccessMediator, AccessComponent}
import org.aphreet.c3.platform.task.{TaskManager, TaskComponent}
import org.aphreet.c3.platform.statistics.{StatisticsManager, StatisticsComponent}
import org.aphreet.c3.platform.search.impl.SearchComponentImpl

/**
 * Author: Mikhail Malygin
 * Date:   12/21/13
 * Time:   4:20 PM
 */
class C3SearchActivator extends C3Activator {

  def name: String = "c3-search"

  def createApplication(context: BundleContext): C3AppHandle = {

    trait DependencyProvider extends  AccessComponent
    with StorageComponent
    with PlatformConfigComponent
    with TaskComponent
    with StatisticsComponent {
      val accessManager = getService(context, classOf[AccessManager])

      val accessMediator = getService(context, classOf[AccessMediator])

      val statisticsManager = getService(context, classOf[StatisticsManager])

      val storageManager = getService(context, classOf[StorageManager])

      val taskManager = getService(context, classOf[TaskManager])

      val platformConfigManager = getService(context, classOf[PlatformConfigManager])

      val configPersister = getService(context, classOf[ConfigPersister])
    }

    val module = new Object with DefaultComponentLifecycle
    with DependencyProvider
    with SearchComponentImpl

    new C3AppHandle {
      def registerServices(context: BundleContext){
        registerService(context, classOf[StorageManager], module.storageManager)
      }

      val app = module
    }
  }
}
