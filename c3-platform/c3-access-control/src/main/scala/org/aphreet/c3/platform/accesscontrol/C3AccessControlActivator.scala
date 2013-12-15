package org.aphreet.c3.platform.accesscontrol

import org.aphreet.c3.platform.access.{CleanupComponent, CleanupManager}
import org.aphreet.c3.platform.accesscontrol.impl.AccessControlComponentImpl
import org.aphreet.c3.platform.auth.impl.AuthenticationComponentImpl
import org.aphreet.c3.platform.common.{DefaultComponentLifecycle, C3Activator}
import org.aphreet.c3.platform.config.EnvironmentSystemDirectoryProvider
import org.aphreet.c3.platform.domain.impl.DomainComponentImpl
import org.osgi.framework.BundleContext
import org.aphreet.c3.platform.domain.DomainManager
import org.aphreet.c3.platform.auth.AuthenticationManager

/**
 * Author: Mikhail Malygin
 * Date:   12/14/13
 * Time:   2:59 AM
 */
class C3AccessControlActivator extends C3Activator {

  var app: Option[DefaultComponentLifecycle] = None

  def start(context: BundleContext) {

    log.info("Starting c3-access-control")

    trait ServiceDependencyProvider extends CleanupComponent{
      val cleanupManager: CleanupManager = getService(context, classOf[CleanupManager])
    }

    log.info("Creating components")

    val app = new Object
      with DefaultComponentLifecycle
      with EnvironmentSystemDirectoryProvider
      with ServiceDependencyProvider
      with DomainComponentImpl
      with AuthenticationComponentImpl
      with AccessControlComponentImpl

    log.info("Initializing components")

    app.start()

    registerService(context, classOf[DomainManager], app.domainManager)
    registerService(context, classOf[AuthenticationManager], app.authenticationManager)
    registerService(context, classOf[AccessControlManager], app.accessControlManager)

    this.app = Some(app)

    log.info("c3-access-control started")

  }

  def stop(context: BundleContext) {
    log.info("Stopping c3-access-control")

    this.app.foreach(_.stop())

    log.info("c3-access-control stopped")
  }
}
