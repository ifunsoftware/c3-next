package org.aphreet.c3.platform.remote.replication

import akka.actor.ActorRefFactory
import com.typesafe.config.{ConfigFactory, Config}
import org.aphreet.c3.platform.access.{AccessManager, AccessMediator, AccessComponent}
import org.aphreet.c3.platform.actor.ActorComponent
import org.aphreet.c3.platform.auth.{AuthenticationManager, AuthenticationComponent}
import org.aphreet.c3.platform.common.{C3ActorActivator, DefaultComponentLifecycle, C3AppHandle}
import org.aphreet.c3.platform.config.{PlatformConfigManager, ConfigPersister, PlatformConfigComponent}
import org.aphreet.c3.platform.domain.{DomainManager, DomainComponent}
import org.aphreet.c3.platform.filesystem.{FSManager, FSComponent}
import org.aphreet.c3.platform.remote.replication.impl.{NetworkSettings, ReplicationComponentImpl}
import org.aphreet.c3.platform.statistics.{StatisticsManager, StatisticsComponent}
import org.aphreet.c3.platform.storage.{StorageManager, StorageComponent}
import org.aphreet.c3.platform.task.{TaskManager, TaskComponent}
import org.osgi.framework.BundleContext

/**
 * Author: Mikhail Malygin
 * Date:   12/27/13
 * Time:   3:13 PM
 */
class C3ReplicationActivator extends C3ActorActivator {
  def name: String = "c3-replication"

  def createApplication(context: BundleContext, actorRefFactory: ActorRefFactory): C3AppHandle = {

    trait DependencyProvider extends PlatformConfigComponent
    with DomainComponent
    with ActorComponent
    with FSComponent
    with AccessComponent
    with StorageComponent
    with TaskComponent
    with StatisticsComponent
    with AuthenticationComponent {

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

      val actorSystem = actorRefFactory
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

  override def getActorSystemName(context: BundleContext): String = "c3-replication"

  override def getActorSystemConfiguration(context: BundleContext): Config = {

    val bindAddress = NetworkSettings.replicationBindAddress
    val bindPort = NetworkSettings.replicationBindPort

    log.info(s"Binding akka remote on $bindAddress:$bindPort")

    ConfigFactory.parseString(
      s"""
        |akka.remote.netty.tcp {
        |  hostname = "$bindAddress"
        |  port = $bindPort
        |}
      """.stripMargin).withFallback(ConfigFactory.load())
  }
}
