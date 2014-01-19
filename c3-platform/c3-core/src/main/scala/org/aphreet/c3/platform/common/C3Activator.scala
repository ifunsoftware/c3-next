package org.aphreet.c3.platform.common

import akka.actor.{ActorSystem, ActorRefFactory}
import akka.osgi.ActorSystemActivator
import com.typesafe.config.{ConfigFactory, Config}
import java.util.Properties
import org.osgi.framework.{BundleActivator, BundleContext}

/**
 * Author: Mikhail Malygin
 * Date:   12/14/13
 * Time:   3:00 AM
 */
trait C3Activator {

  val log = Logger(getClass)

  def name: String

  protected def app: Option[C3AppHandle]

  protected def app_=(value: Option[C3AppHandle]): Unit

  def init(context: BundleContext, appCreator: Unit => C3AppHandle) {
    log.info("Starting {}", name)

    val handle: C3AppHandle = appCreator()

    log.info("Initializing {}", name)

    handle.start()

    log.info("Registering services for {} ", name)

    handle.registerServices(context)

    log.info("{} started", name)

    app = Some(handle)
  }

  def destroy(context: BundleContext) {
    log.info("Stopping {}", name)

    app.map(_.stop())

    log.info("{}, stopped", name)
  }

  def registerService[T](context: BundleContext, clazz: Class[T], service: T) {
    context.registerService(clazz.getCanonicalName, service, new Properties())
  }

  def getService[T](context: BundleContext, clazz: Class[T]): T = {
    val reference = context.getServiceReference(clazz.getCanonicalName)

    context.getService(reference).asInstanceOf[T]
  }
}

abstract class C3ActorActivator extends ActorSystemActivator with C3Activator {

  var app: Option[C3AppHandle] = None

  def createApplication(context: BundleContext, actorRefFactory: ActorRefFactory): C3AppHandle

  def configure(context: BundleContext, system: ActorSystem) {
    init(context, Unit => createApplication(context, system))
  }

  override def stop(context: BundleContext) {
    destroy(context)
    super.destroy(context)
  }

  override def getActorSystemConfiguration(context: BundleContext): Config = {
    ConfigFactory.parseString("akka.loggers = [\"org.aphreet.c3.platform.actor.Slf4jLogger\"]")
  }
}

abstract class C3SimpleActivator extends BundleActivator with C3Activator {

  var app: Option[C3AppHandle] = None

  def createApplication(context: BundleContext): C3AppHandle

  override def start(context: BundleContext) {
    init(context, Unit => createApplication(context))
  }

  override def stop(context: BundleContext) {
    destroy(context)
    super.destroy(context)
  }
}

trait C3AppHandle {

  def registerServices(context: BundleContext)

  val app: DefaultComponentLifecycle

  def start() {
    app.start()
  }

  def stop() {
    app.stop()
  }

}
