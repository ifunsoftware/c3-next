package org.aphreet.c3.platform.accesscontrol

import org.aphreet.c3.platform.access.{CleanupComponent, CleanupManager}
import org.aphreet.c3.platform.accesscontrol.impl.AccessControlComponentImpl
import org.aphreet.c3.platform.auth.AuthenticationManager
import org.aphreet.c3.platform.auth.impl.AuthenticationComponentImpl
import org.aphreet.c3.platform.common.{C3AppHandle, DefaultComponentLifecycle, C3Activator}
import org.aphreet.c3.platform.config.{PlatformConfigManager, ConfigPersister, PlatformConfigComponent}
import org.aphreet.c3.platform.domain.DomainManager
import org.aphreet.c3.platform.domain.impl.DomainComponentImpl
import org.osgi.framework.BundleContext
import akka.actor.ActorRefFactory

/**
 * Author: Mikhail Malygin
 * Date:   12/14/13
 * Time:   2:59 AM
 */
class C3AccessControlActivator extends C3Activator {


  def name = "c3-access-control"

  def createApplication(context: BundleContext, actorRefFactory: ActorRefFactory): C3AppHandle = {

    trait ServiceDependencyProvider extends CleanupComponent with PlatformConfigComponent{
      val cleanupManager = getService(context, classOf[CleanupManager])

      val platformConfigManager = getService(context, classOf[PlatformConfigManager])

      val configPersister = getService(context, classOf[ConfigPersister])
    }

    val module = new Object
      with DefaultComponentLifecycle
      with ServiceDependencyProvider
      with DomainComponentImpl
      with AuthenticationComponentImpl
      with AccessControlComponentImpl

    new C3AppHandle {
      def registerServices(context: BundleContext){
        registerService(context, classOf[DomainManager], module.domainManager)
        registerService(context, classOf[AuthenticationManager], module.authenticationManager)
        registerService(context, classOf[AccessControlManager], module.accessControlManager)
      }

      val app = module
    }
  }
}
