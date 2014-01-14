package org.aphreet.c3.platform.common

import org.osgi.framework.{BundleActivator, BundleContext}
import java.util.Properties
import akka.actor.{ActorSystem, ActorRef, ActorRefFactory}
import akka.osgi.ActorSystemActivator

/**
 * Author: Mikhail Malygin
 * Date:   12/14/13
 * Time:   3:00 AM
 */
trait C3Activator extends ActorSystemActivator {

  val log = Logger(getClass)

  var app: Option[C3AppHandle] = None

  def name: String

  def createApplication(context: BundleContext, actorRefFactory: ActorRefFactory): C3AppHandle


  def configure(context: BundleContext, system: ActorSystem) {
    log.info("Starting {}", name)

    val handle = createApplication(context, system)

    log.info("Initializing {}", name)

    handle.start()

    log.info("Registering services for {} ", name)

    handle.registerServices(context)

    log.info("{} started", name)

    app = Some(handle)
  }

  override def stop(context: BundleContext) {
    log.info("Stopping {}", name)

    app.foreach(_.stop())

    log.info("{}, stopped", name)

    super.stop(context)
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
