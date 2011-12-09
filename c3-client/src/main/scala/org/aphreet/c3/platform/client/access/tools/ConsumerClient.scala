/**
 * Copyright (c) 2010, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.aphreet.c3.platform.client.access.tools

import java.util.concurrent.{ArrayBlockingQueue, Executors}
import java.io.{File, FileReader, BufferedReader}
import org.aphreet.c3.platform.client.common.CLI
import org.aphreet.c3.platform.client.common.ArgumentType._
import worker.ConsumerWorker


abstract class ConsumerClient(override val args:Array[String]) extends CLI(args){

  def cliDescription = parameters(
    HOST_ARG,
    USER_ARG,
    KEY_ARG,
    THREADS_ARG,
    IN_ARG,
    HELP_ARG
    )

  def run(){
    val file:String = IN_ARG

    if(file == null){
      throw new IllegalAccessException("File argument is mandatory")
    }

    parseCLI()

    consumeResources(HOST_ARG, USER_ARG, KEY_ARG, THREADS_ARG, file)
  }

  def parseCLI(){}

  def consumeResources(host:String, user:String, key:String, threads:Int, file:String){

    println("Starting " + clientName + "...")

    val queue = new ArrayBlockingQueue[String](threads * 5)

    var consumers:List[ConsumerWorker] = List()
    for(i <- 1 to threads){
      consumers = createConsumer(host, user, key, queue) :: consumers
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

    addressReader.close()
    executor.shutdown()

  }

  def isRunning(consumers:List[ConsumerWorker]):Boolean = consumers.exists(!_.done)

  def processed(consumers:List[ConsumerWorker]):Int = consumers.map(e => e.processed).foldLeft(0)(_ + _)

  def errors(consumers:List[ConsumerWorker]):Int = consumers.map(e => e.errors).foldLeft(0)(_ + _)

  def actionName:String

  def createConsumer(host:String, user:String, key:String, queue:ArrayBlockingQueue[String]):ConsumerWorker
}