package org.aphreet.c3.platform.common

import scala.collection.mutable.ArrayBuffer

/**
 * Author: Mikhail Malygin
 * Date:   12/9/13
 * Time:   6:32 PM
 */
trait ComponentLifecycle {

  type LifecycleCallback = Unit => Unit

  def init(callback: LifecycleCallback)

  def destroy(callback: LifecycleCallback)

}

trait DefaultComponentLifecycle extends ComponentLifecycle{

  private val initCallbacks = new ArrayBuffer[LifecycleCallback]()
  private val destroyCallbacks = new ArrayBuffer[LifecycleCallback]()

  def init(callback: LifecycleCallback) {
    initCallbacks.synchronized{
      initCallbacks.append(callback)
    }
  }

  def destroy(callback: LifecycleCallback) {
    destroyCallbacks.synchronized{
      destroyCallbacks.prepend(callback)
    }
  }

  def start(){
    initCallbacks.foreach(callback => {
      try{
        callback()
      }catch{
        case e: Throwable =>
          DefaultComponentLifecycle.logger.error("Failed to init component with callback " + callback, e)
          throw e
      }
    })
  }

  def stop(){
    destroyCallbacks.foreach(callback => {
      try{
        callback()
      }catch {
        case e: Throwable =>
          DefaultComponentLifecycle.logger.error("Failed to destroy component via callback " + callback, e)
      }
    })
  }
}

object DefaultComponentLifecycle{

  val logger = Logger(classOf[DefaultComponentLifecycle])

}
