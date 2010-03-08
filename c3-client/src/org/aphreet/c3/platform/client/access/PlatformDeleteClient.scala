package org.aphreet.c3.platform.client.access

import java.util.concurrent.ArrayBlockingQueue
import worker.{DeleteWorker, ConsumerWorker}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 8, 2010
 * Time: 10:41:09 PM
 * To change this template use File | Settings | File Templates.
 */

class PlatformDeleteClient(override val args:Array[String]) extends ConsumerClient(args){

  def clientName = "Deleter"

  def actionName = "deleted"

  def createConsumer(host:String, queue:ArrayBlockingQueue[String]):ConsumerWorker = new DeleteWorker(host, queue)

}