package org.aphreet.c3.platform.client.access

import org.aphreet.c3.platform.client.access.http.C3HttpAccessor
import java.util.Random
import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 7, 2010
 * Time: 1:30:48 AM
 * To change this template use File | Settings | File Templates.
 */

class ResourceWriter(val host:String, val count:Int) extends Runnable {

  var _size:Int = 1024
  var _md:Map[String, String] = Map()
  var _queue:LinkedBlockingQueue[String] = null
  var written:Int = 0
  var errors:Int = 0
  var done:Boolean = false

  def size(s:Int):ResourceWriter = {_size = s; this}

  def metadata(md:Map[String, String]):ResourceWriter = {_md = md; this}

  def queue(queue:LinkedBlockingQueue[String]):ResourceWriter = {_queue = queue; this}

  override def run{

    val client = new C3HttpAccessor(host)

    for(i <- 1 to count){
      try{
        val ra = client.write(generateDataOfSize(_size), _md)
        if(_queue != null){
          _queue.offer(ra)
        }
        written = written + 1
      }catch{
        case e => {errors = errors + 1; System.err.println(e.getMessage)}
      }
    }

    done = true
  }

  def generateDataOfSize(size:Int):Array[Byte] = {

    val result = new Array[Byte](size)
    val random = new Random(System.currentTimeMillis)
    random.nextBytes(result)

    result
  }
  
}