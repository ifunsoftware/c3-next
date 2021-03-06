package org.aphreet.c3.platform.filesystem

import akka.actor.ActorRefFactory
import org.aphreet.c3.platform.access.{AccessComponent, AccessMediator, AccessManager}
import org.aphreet.c3.platform.actor.ActorComponent
import org.aphreet.c3.platform.common.{C3ActorActivator, C3AppHandle, DefaultComponentLifecycle}
import org.aphreet.c3.platform.config.{PlatformConfigComponent, ConfigPersister, PlatformConfigManager}
import org.aphreet.c3.platform.filesystem.impl.{FSComponentImpl, FSCleanupComponentImpl}
import org.aphreet.c3.platform.metadata.{TransientMetadataComponent, TransientMetadataManager}
import org.aphreet.c3.platform.query.{QueryComponent, QueryManager}
import org.aphreet.c3.platform.statistics.{StatisticsManager, StatisticsComponent}
import org.aphreet.c3.platform.storage.{StorageComponent, StorageManager}
import org.aphreet.c3.platform.task.{TaskComponent, TaskManager}
import org.osgi.framework.BundleContext

/**
 * Author: Mikhail Malygin
 * Date:   12/18/13
 * Time:   12:01 AM
 */
class C3FilesystemActivator extends C3ActorActivator {
  def name = "c3-filesystem"

  def createApplication(context: BundleContext, actorRefFactory: ActorRefFactory): C3AppHandle = {

    trait DependencyProvider extends AccessComponent
    with StorageComponent
    with ActorComponent
    with TaskComponent
    with QueryComponent
    with TransientMetadataComponent
    with PlatformConfigComponent
    with StatisticsComponent {
      val accessManager = getService(context, classOf[AccessManager])

      val accessMediator = getService(context, classOf[AccessMediator])

      val storageManager = getService(context, classOf[StorageManager])

      val taskManager = getService(context, classOf[TaskManager])

      val queryManager = getService(context, classOf[QueryManager])

      val transientMetadataManager = getService(context, classOf[TransientMetadataManager])

      val platformConfigManager = getService(context, classOf[PlatformConfigManager])

      val configPersister = getService(context, classOf[ConfigPersister])

      val statisticsManager = getService(context, classOf[StatisticsManager])

      val actorSystem = actorRefFactory
    }

    val module = new Object
      with DefaultComponentLifecycle
      with DependencyProvider
      with FSCleanupComponentImpl
      with FSComponentImpl

    new C3AppHandle {
      def registerServices(context: BundleContext) {
        registerService(context, classOf[FSManager], module.filesystemManager)
      }

      val app = module
    }
  }
}
