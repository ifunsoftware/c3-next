package org.aphreet.c3.platform.common

import org.osgi.framework.{BundleActivator, BundleContext}
import java.util.Properties

/**
 * Author: Mikhail Malygin
 * Date:   12/14/13
 * Time:   3:00 AM
 */
trait C3Activator extends BundleActivator {

  val log = Logger(getClass)

  def registerService[T](context: BundleContext, clazz: Class[T], service: T){
    context.registerService(clazz.getCanonicalName, service, new Properties())
  }

  def getService[T](context: BundleContext, clazz: Class[T]): T = {
    val reference = context.getServiceReference(clazz.getCanonicalName)

    context.getService(reference).asInstanceOf[T]
  }

}
