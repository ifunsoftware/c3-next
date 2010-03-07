package org.aphreet.c3.platform.client.access

import org.aphreet.c3.platform.client.common.CLI
import org.aphreet.c3.platform.client.common.ArgumentType._
import java.util.concurrent.{ArrayBlockingQueue, Executors, LinkedBlockingQueue}
import java.io._

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 8, 2010
 * Time: 1:03:37 AM
 * To change this template use File | Settings | File Templates.
 */

class PlatformReadClient(override val args:Array[String]) extends CLI(args){

  def cliDescription = parameters(
    "h" has mandatory argument "host" described "Host to connect to",
    "threads" has mandatory argument "num" described "Thread count",
    "in" has mandatory argument "file" described "Addresses to read",
    "help" described "Prints this message"
    )

  def run{
    if(cli.getOptions.length == 0) helpAndExit("Reader")

    if(cli.hasOption("help")) helpAndExit("Reader")

    val threadCount = cliValue("threads", "1").toInt
    val host = cliValue("host", "http://localhost:8088/c3-remote/")
    val file = cliValue("in", null)

    if(file == null){
      throw new IllegalAccessException("File argument is mandatory")
    }

    readResources(host, threadCount, file)
  }

  def readResources(host:String, threads:Int, file:String){

    println("Reading resources")

    val queue = new ArrayBlockingQueue[String](threads * 5)

    var readers:List[ResourceReader] = List()
    for(i <- 1 to threads){
      readers = new ResourceReader(host, queue) :: readers
    }


    val executor = Executors.newFixedThreadPool(threads)

    val startTime = System.currentTimeMillis
    var time = startTime;
    var totalRead = 0

    readers.foreach(executor.submit(_))

    var addressReader = new BufferedReader(new FileReader(new File(file)))


    var ra = addressReader.readLine
    while(isRunning(readers)){
      var shouldTry = true
      while(ra != null && shouldTry){
        if(queue.offer(ra)) ra = addressReader.readLine
        else shouldTry = false
      }

      val currentTime = System.currentTimeMillis
      val dif = (currentTime - time)/1000
      if(dif >= 10){
        time = currentTime
        val readResources = read(readers)

        val rate = (readResources - totalRead).asInstanceOf[Float] / (dif)
        val avgRate = (readResources).asInstanceOf[Float] / ((time - startTime)/1000)

        totalRead = readResources

        println("Read " + readResources + " resources (" + rate + ", " + avgRate +")")
      }
    }

    val endTime = System.currentTimeMillis
    println(read(readers) + " resources read in " + (endTime - startTime)/1000)

    addressReader.close
    executor.shutdown

  }

  def isRunning(readers:List[ResourceReader]):Boolean = readers.exists(!_.done)

  def read(readers:List[ResourceReader]):Int = readers.map(e => e.read).foldLeft(0)(_ + _)

}