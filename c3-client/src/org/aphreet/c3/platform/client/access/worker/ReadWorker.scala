package org.aphreet.c3.platform.client.access.worker

import java.util.concurrent.ArrayBlockingQueue

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 8, 2010
 * Time: 10:26:18 PM
 * To change this template use File | Settings | File Templates.
 */

class ReadWorker(override val host:String,
                   override val user:String,
                   override val key:String,
                   override val queue:ArrayBlockingQueue[String])
        extends ConsumerWorker(host, user, key, queue){

  override def execute(address:String) = client.fakeRead(address)
}