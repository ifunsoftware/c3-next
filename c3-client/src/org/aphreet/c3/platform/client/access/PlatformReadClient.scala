package org.aphreet.c3.platform.client.access

import java.util.concurrent.ArrayBlockingQueue
import worker.{ReadWorker, ConsumerWorker}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 8, 2010
 * Time: 1:03:37 AM
 * To change this template use File | Settings | File Templates.
 */

class PlatformReadClient(override val args:Array[String]) extends ConsumerClient(args){

  def clientName = "Reader"

  def actionName = "read"

  def createConsumer(host:String, queue:ArrayBlockingQueue[String]):ConsumerWorker = new ReadWorker(host, queue)

}