package org.aphreet.c3.platform.client.access.worker

import java.util.concurrent.{TimeUnit, ArrayBlockingQueue}
import org.aphreet.c3.platform.client.access.http.C3HttpAccessor

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 8, 2010
 * Time: 10:22:51 PM
 * To change this template use File | Settings | File Templates.
 */

class ConsumerWorker(val host:String, val queue:ArrayBlockingQueue[String]) extends Runnable{

  var done:Boolean = false
  var processed:Int = 0
  var errors:Int = 0
  var bytesRead:Long = 0l
  val client = new C3HttpAccessor(host)

  override def run{

    var address = queue.poll(5, TimeUnit.SECONDS)

    while(address != null){
      try{
        val bytes = execute(address)
        bytesRead = bytesRead + bytes
        processed = processed + 1
      }catch{
        case e => {
          errors = errors + 1
          System.err.println("Error: " + e.getMessage)
        }
      }
      address = queue.poll(5, TimeUnit.SECONDS)
    }

    done = true
  }

  def execute(address:String):Long = {
    client.fakeRead(address) 
  }

  
}