package org.aphreet.c3.platform.remote.replication.impl

import org.aphreet.c3.platform.remote.replication.ReplicationComponent
import org.aphreet.c3.platform.config.PlatformConfigComponent
import org.aphreet.c3.platform.remote.replication.impl.config._
import org.aphreet.c3.platform.domain.DomainComponent
import org.aphreet.c3.platform.filesystem.FSComponent
import org.aphreet.c3.platform.access.AccessComponent
import org.aphreet.c3.platform.storage.StorageComponent
import org.aphreet.c3.platform.task.TaskComponent
import org.aphreet.c3.platform.statistics.StatisticsComponent
import org.aphreet.c3.platform.auth.AuthenticationComponent
import org.aphreet.c3.platform.common.ComponentLifecycle
import org.aphreet.c3.platform.actor.ActorComponent
import akka.actor.Props

/**
 * Author: Mikhail Malygin
 * Date:   12/25/13
 * Time:   9:54 PM
 */
trait ReplicationComponentImpl extends ReplicationComponent {

  this: PlatformConfigComponent
    with DomainComponent
    with ActorComponent
    with FSComponent
    with AccessComponent
    with StorageComponent
    with TaskComponent
    with StatisticsComponent
    with AuthenticationComponent
    with ComponentLifecycle
    =>

  val replicationConfigurationManager = new ConfigurationManager(filesystemManager, domainManager, platformConfigManager)

  val replicationManager = new ReplicationManagerImpl(actorSystem, accessMediator, storageManager, taskManager, domainManager,
    statisticsManager, platformConfigManager, configPersister, replicationConfigurationManager)

  val replicationNegotiator = actorSystem.actorOf(Props.create(classOf[ReplicationNegotiatorServer], authenticationManager, replicationConfigurationManager,
    replicationManager), ReplicationNegotiator.ACTOR_NAME)

  destroy(Unit => replicationManager.destroy())

}
