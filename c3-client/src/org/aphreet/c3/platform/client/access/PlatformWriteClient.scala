package org.aphreet.c3.platform.client.access

import org.aphreet.c3.platform.client.access.http.C3HttpAccessor
import org.aphreet.c3.platform.client.common.CLI
import org.aphreet.c3.platform.client.common.ArgumentType._
import java.util.Random
import java.util.concurrent.{Executors, LinkedBlockingQueue}
import java.io.{BufferedOutputStream, File, FileOutputStream}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 6, 2010
 * Time: 11:09:00 PM
 * To change this template use File | Settings | File Templates.
 */

class PlatformWriteClient(override val args:Array[String]) extends CLI(args){

  def cliDescription = parameters(
    "h" has mandatory argument "host" described "Host to connect to",
    "size" has mandatory argument "num" described "Size of object to write",
    "count" has mandatory argument "num" described "Count of objects to write",
    "threads" has mandatory argument "num" described "Thread count",
    "type" has mandatory argument "mime" described "Mime type of content",
    "pool" has mandatory argument "name" described "Target pool",
    "help" described "Prints this message"
  )

  def run{
    if(cli.getOptions.length == 0) helpAndExit("Writer")

    if(cli.hasOption("help")) helpAndExit("Writer" )

    val objectSize = cliValue("size", "512").toInt
    val objectCount = cliValue("count", "-1").toInt
    val threadCount = cliValue("threads", "1").toInt
    val objectType = cliValue("type", "application/octet-stream")
    val pool = cliValue("pool", "")
    val host = cliValue("host", "http://localhost:8088/c3-remote/")



    if(objectCount < 0)
      throw new IllegalArgumentException("Object count is not set")

    writeObjects(host, objectCount, objectSize, threadCount, Map("pool" -> pool, "content.type" -> objectType))
  }

  def writeObjects(host:String, count:Int, size:Int, threads:Int, metadata:Map[String, String]){

    println("Writing " + count + " objects of size " + size)

    val queue = new LinkedBlockingQueue[String]

    var writers:List[ResourceWriter] = List()

    val perThread = count / threads
    val rest = count % threads

    for(i <- 1 to threads){
      val toWrite = if(i == threads){
        perThread + rest
      }else{
        perThread
      }
      writers = new ResourceWriter(host, toWrite).size(size).metadata(metadata).queue(queue) :: writers
    }


    val executor = Executors.newFixedThreadPool(threads)

    val startTime = System.currentTimeMillis
    var time = startTime;

    writers.foreach(executor.submit(_))

    val fos = new BufferedOutputStream(new FileOutputStream(new File("/Users/Aphreet/resources.out")))

    while(isRunning(writers)){
      var ra = queue.poll
      while(ra != null){
        fos.write((ra + "\n").getBytes)
        ra = queue.poll
      }
      val currentTime = System.currentTimeMillis
      if(currentTime - time > 10000){
        time = currentTime
        println("Written " + written(writers) + " objects")
      }
    }

    val endTime = System.currentTimeMillis
    println(count + " objects written in " + (endTime - startTime)/1000)

    fos.close
    executor.shutdown
    

//    val client = new C3HttpAccessor(host)
//
//
//    println("Writing " + count + " objects of size " + size)
//
//    val startTime = System.currentTimeMillis
//    var time = startTime;
//
//    for(i <- 1 to count){
//
//      val ra = client.write(generateDataOfSize(size), metadata)
//
//      println(ra)
//
//      if(i % 1000 == 0){
//        val wtime = System.currentTimeMillis
//        val rate = 1000f/((wtime - time)/1000f)
//        println("Saved " + i + " objects with average rate " + rate + "(obj/s)")
//        time = System.currentTimeMillis
//      }
//    }
//
//    val endTime = System.currentTimeMillis
//
//    println(count + " objects written in " + (endTime - startTime)/1000)
  }

  def isRunning(writers:List[ResourceWriter]):Boolean = writers.exists(!_.done)

  def written(writers:List[ResourceWriter]):Int = writers.map(e => e.written).foldLeft(0)(_ + _)
}