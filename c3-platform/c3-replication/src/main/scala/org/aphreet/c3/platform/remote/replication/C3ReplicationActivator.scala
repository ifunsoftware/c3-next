package org.aphreet.c3.platform.remote.replication

import org.aphreet.c3.platform.common.{DefaultComponentLifecycle, C3AppHandle, C3Activator}
import org.osgi.framework.BundleContext
import org.aphreet.c3.platform.config.{PlatformConfigManager, ConfigPersister, PlatformConfigComponent}
import org.aphreet.c3.platform.filesystem.{FSManager, FSComponent}
import org.aphreet.c3.platform.domain.{DomainManager, DomainComponent}
import org.aphreet.c3.platform.access.{AccessManager, AccessMediator, AccessComponent}
import org.aphreet.c3.platform.storage.{StorageManager, StorageComponent}
import org.aphreet.c3.platform.task.{TaskManager, TaskComponent}
import org.aphreet.c3.platform.statistics.{StatisticsManager, StatisticsComponent}
import org.aphreet.c3.platform.auth.{AuthenticationManager, AuthenticationComponent}
import org.aphreet.c3.platform.remote.replication.impl.ReplicationComponentImpl

/**
 * Author: Mikhail Malygin
 * Date:   12/27/13
 * Time:   3:13 PM
 */
class C3ReplicationActivator extends C3Activator {
  def name: String = "c3-replication"

  def createApplication(context: BundleContext): C3AppHandle = {

    trait DependencyProvider extends PlatformConfigComponent
      with DomainComponent
      with FSComponent
      with AccessComponent
      with StorageComponent
      with TaskComponent
      with StatisticsComponent
      with AuthenticationComponent{

      val authenticationManager = getService(context, classOf[AuthenticationManager])

      val accessManager = getService(context, classOf[AccessManager])

      val accessMediator = getService(context, classOf[AccessMediator])

      val domainManager = getService(context, classOf[DomainManager])

      val statisticsManager = getService(context, classOf[StatisticsManager])

      val storageManager = getService(context, classOf[StorageManager])

      val taskManager = getService(context, classOf[TaskManager])

      val platformConfigManager = getService(context, classOf[PlatformConfigManager])

      val configPersister = getService(context, classOf[ConfigPersister])

      val filesystemManager = getService(context, classOf[FSManager])
    }

    val module = new Object with DefaultComponentLifecycle
      with DependencyProvider
      with ReplicationComponentImpl

    new C3AppHandle {
      def registerServices(context: BundleContext): Unit = {
        registerService(context, classOf[ReplicationManager], module.replicationManager)
      }

      val app = module
    }
  }
}
