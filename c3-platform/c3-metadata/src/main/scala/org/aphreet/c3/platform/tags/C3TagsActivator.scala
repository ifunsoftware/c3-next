package org.aphreet.c3.platform.tags

import org.aphreet.c3.platform.common.{DefaultComponentLifecycle, C3AppHandle, C3Activator}
import org.osgi.framework.BundleContext
import org.aphreet.c3.platform.access.{AccessMediator, AccessManager, AccessComponent}
import org.aphreet.c3.platform.tags.impl.TagComponentImpl
import akka.actor.{ActorRefFactory, ActorSystem}
import org.aphreet.c3.platform.actor.ActorComponent

/**
 * Author: Mikhail Malygin
 * Date:   12/25/13
 * Time:   8:38 PM
 */
class C3TagsActivator extends C3Activator {
  def name: String = "c3-metadata"

  def createApplication(context: BundleContext, actorRefFactory: ActorRefFactory): C3AppHandle = {

    trait DependencyProvider extends AccessComponent with ActorComponent {

      val actorSystem = actorRefFactory

      val accessManager = getService(context, classOf[AccessManager])

      val accessMediator = getService(context, classOf[AccessMediator])

    }

    val module = new Object with DefaultComponentLifecycle
    with DependencyProvider
    with TagComponentImpl

    new C3AppHandle {
      def registerServices(context: BundleContext) {}

      val app = module
    }

  }
}
