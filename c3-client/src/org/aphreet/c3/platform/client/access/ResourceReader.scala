package org.aphreet.c3.platform.client.access

import http.C3HttpAccessor
import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 8, 2010
 * Time: 1:07:51 AM
 * To change this template use File | Settings | File Templates.
 */

class ResourceReader(val host:String, val queue:ArrayBlockingQueue[String]) extends Runnable{

  var done:Boolean = false
  var read:Int = 0
  var bytesRead:Long = 0l

  override def run{

    val client = new C3HttpAccessor(host)

    var address = queue.poll(10, TimeUnit.SECONDS)

    while(address != null){
      bytesRead = bytesRead + client.fakeRead(address)
      read = read + 1
      address = queue.poll(10, TimeUnit.SECONDS)
    }

    done = true
  }

}