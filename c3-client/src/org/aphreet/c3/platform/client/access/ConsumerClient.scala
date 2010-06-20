package org.aphreet.c3.platform.client.access

import java.util.concurrent.{ArrayBlockingQueue, Executors}
import java.io.{File, FileReader, BufferedReader}
import org.aphreet.c3.platform.client.common.CLI
import org.aphreet.c3.platform.client.common.ArgumentType._
import worker.ConsumerWorker

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 8, 2010
 * Time: 10:29:11 PM
 * To change this template use File | Settings | File Templates.
 */

abstract class ConsumerClient(override val args:Array[String]) extends CLI(args){

  def cliDescription = parameters(
    "host" has mandatory argument "host" described "Host to connect to",
    "threads" has mandatory argument "num" described "Thread count",
    "in" has mandatory argument "file" described "File with resource addresses",
    "help" described "Prints this message"
    )

  def run{
    if(cli.getOptions.length == 0) helpAndExit(clientName)

    if(cli.hasOption("help")) helpAndExit(clientName)

    val threadCount = cliValue("threads", "1").toInt
    val host = "http://" + cliValue("host", "localhost:8080") + "/c3-remote/resource/"
    val file = cliValue("in", null)

    if(file == null){
      throw new IllegalAccessException("File argument is mandatory")
    }

    consumeResources(host, threadCount, file)
  }

  def consumeResources(host:String, threads:Int, file:String){

    println("Starting " + clientName + "...")

    val queue = new ArrayBlockingQueue[String](threads * 5)

    var consumers:List[ConsumerWorker] = List()
    for(i <- 1 to threads){
      consumers = createConsumer(host, queue) :: consumers
    }


    val executor = Executors.newFixedThreadPool(threads)

    val startTime = System.currentTimeMillis
    var time = startTime;
    var totalProcessed = 0

    consumers.foreach(executor.submit(_))

    var addressReader = new BufferedReader(new FileReader(new File(file)))


    var ra = addressReader.readLine
    while(isRunning(consumers)){
      var shouldTry = true
      while(ra != null && shouldTry){
        if(queue.offer(ra)) ra = addressReader.readLine
        else shouldTry = false
      }

      val currentTime = System.currentTimeMillis
      val dif = (currentTime - time)/1000
      if(dif >= 10){
        time = currentTime
        val processedResources = processed(consumers)

        val rate = (processedResources - totalProcessed).asInstanceOf[Float] / (dif)
        val avgRate = (processedResources).asInstanceOf[Float] / ((time - startTime)/1000)

        totalProcessed = processedResources

        println(actionName + " " + processedResources + " resources (" + rate + ", " + avgRate +") errors: "+ errors(consumers))
      }
    }

    val endTime = System.currentTimeMillis
    println(processed(consumers) + " resources " + actionName + " in " + (endTime - startTime)/1000)
    println("Done")

    addressReader.close
    executor.shutdown

  }

  def isRunning(consumers:List[ConsumerWorker]):Boolean = consumers.exists(!_.done)

  def processed(consumers:List[ConsumerWorker]):Int = consumers.map(e => e.processed).foldLeft(0)(_ + _)

  def errors(consumers:List[ConsumerWorker]):Int = consumers.map(e => e.errors).foldLeft(0)(_ + _)


  def clientName:String

  def actionName:String

  def createConsumer(host:String, queue:ArrayBlockingQueue[String]):ConsumerWorker
}