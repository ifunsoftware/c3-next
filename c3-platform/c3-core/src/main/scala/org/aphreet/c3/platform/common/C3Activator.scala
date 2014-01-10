package org.aphreet.c3.platform.common

import org.osgi.framework.{BundleActivator, BundleContext}
import java.util.Properties
import akka.actor.{ActorRef, ActorRefFactory}

/**
 * Author: Mikhail Malygin
 * Date:   12/14/13
 * Time:   3:00 AM
 */
trait C3Activator extends BundleActivator {

  val log = Logger(getClass)

  var app: Option[C3AppHandle] = None

  def name: String

  def createApplication(context: BundleContext): C3AppHandle

  def start(context: BundleContext) {

    log.info("Starting {}", name)

    val handle = createApplication(context)

    log.info("Initializing {}", name)

    handle.start()

    log.info("Registering services for {} ", name)

    handle.registerServices(context)

    log.info("{} started", name)

    app = Some(handle)
  }

  def stop(context: BundleContext) {
    log.info("Stopping {}", name)

    app.foreach(_.stop())

    log.info("{}, stopped", name)
  }

  def registerService[T](context: BundleContext, clazz: Class[T], service: T){
    context.registerService(clazz.getCanonicalName, service, new Properties())
  }

  def getService[T](context: BundleContext, clazz: Class[T]): T = {
    val reference = context.getServiceReference(clazz.getCanonicalName)

    context.getService(reference).asInstanceOf[T]
  }
}

trait C3AppHandle {

  def registerServices(context: BundleContext)

  val app: DefaultComponentLifecycle

  def start() {
    app.start()
  }

  def stop(){
    app.stop()
  }

}
