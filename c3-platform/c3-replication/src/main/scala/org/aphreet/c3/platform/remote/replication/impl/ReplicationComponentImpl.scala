package org.aphreet.c3.platform.remote.replication.impl

import org.aphreet.c3.platform.remote.replication.ReplicationComponent
import org.aphreet.c3.platform.config.PlatformConfigComponent
import org.aphreet.c3.platform.remote.replication.impl.config.{ReplicationNegotiator, ConfigurationManager, ReplicationTargetsConfigAccessor, ReplicationSourcesConfigAccessor}
import org.aphreet.c3.platform.domain.DomainComponent
import org.aphreet.c3.platform.filesystem.FSComponent
import org.aphreet.c3.platform.access.AccessComponent
import org.aphreet.c3.platform.storage.StorageComponent
import org.aphreet.c3.platform.task.TaskComponent
import org.aphreet.c3.platform.statistics.StatisticsComponent
import org.aphreet.c3.platform.auth.AuthenticationComponent
import org.aphreet.c3.platform.common.ComponentLifecycle

/**
 * Author: Mikhail Malygin
 * Date:   12/25/13
 * Time:   9:54 PM
 */
trait ReplicationComponentImpl extends ReplicationComponent {

  this: PlatformConfigComponent
    with DomainComponent
    with FSComponent
    with AccessComponent
    with StorageComponent
    with TaskComponent
    with StatisticsComponent
    with AuthenticationComponent
    with ComponentLifecycle
    =>

  val configurationManager = new ConfigurationManager(filesystemManager, domainManager, platformConfigManager)

  val replicationPortRetriever = new ReplicationPortRetriever

  val replicationManager = new ReplicationManagerImpl(accessMediator, storageManager, taskManager, domainManager,
    statisticsManager, platformConfigManager, configPersister, configurationManager, replicationPortRetriever)

  val replicationNegotiator = new ReplicationNegotiator(authenticationManager, configurationManager,
    replicationManager, replicationPortRetriever)

  destroy(Unit => replicationManager.destroy())
  destroy(Unit => replicationNegotiator.destroy())

}
