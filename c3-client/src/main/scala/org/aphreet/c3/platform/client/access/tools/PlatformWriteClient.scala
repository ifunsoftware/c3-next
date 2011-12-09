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

import org.aphreet.c3.platform.client.common.CLI
import org.aphreet.c3.platform.client.common.ArgumentType._
import java.util.concurrent.{Executors, LinkedBlockingQueue}
import java.io.{OutputStream, BufferedOutputStream, File, FileOutputStream}

class PlatformWriteClient(override val args: Array[String]) extends CLI(args) {

 

  def clientName = "Writer"

  def cliDescription = parameters(
    HOST_ARG,
    POOL_ARG,
    USER_ARG,
    KEY_ARG,
    SIZE_ARG,
    COUNT_ARG,
    THREADS_ARG,
    TYPE_ARG,
    OUT_ARG,
    HELP_ARG
    )

  def run() {
    val objectType:String = TYPE_ARG
    val pool:String = POOL_ARG

    //    println(pool.getBytes.toString)
    //    println(pool.getBytes("UTF-8").toString)
    //    pool = new String(pool.getBytes("UTF-8"))
    //    println(pool)
    //This is kind of magic
    //This code works for UTF-8 locale
    //But i don't know what will happen on windows machines
    //Possibly
    //pool = new String(pool.getBytes, "UTF-8")


    writeObjects(HOST_ARG, USER_ARG, KEY_ARG, COUNT_ARG, SIZE_ARG, THREADS_ARG, Map("c3.pool" -> pool, "content.type" -> objectType), OUT_ARG)
  }

  def writeObjects(host: String, user:String, key:String, count: Int, size: Int, threads: Int, metadata: Map[String, String], file: String) {

    println("Writing " + count + " objects of size " + size)

    val queue = new LinkedBlockingQueue[String]

    var writers: List[ResourceWriter] = List()

    val perThread = count / threads
    val rest = count % threads

    for (i <- 1 to threads) {
      val toWrite = if (i == threads) {
        perThread + rest
      } else {
        perThread
      }
      writers = new ResourceWriter(host, user, key, toWrite).size(size).metadata(metadata).queue(queue) :: writers
    }


    val executor = Executors.newFixedThreadPool(threads)

    val startTime = System.currentTimeMillis
    var time = startTime;
    var totalWritten = 0

    writers.foreach(executor.submit(_))

    var fos: OutputStream = null

    if (file != null) {

      val fileHandle = new File(file)
      if (fileHandle.exists) fileHandle.delete

      fos = new BufferedOutputStream(new FileOutputStream(new File(file)))
    }

    while (isRunning(writers)) {
      var ra = queue.poll
      while (ra != null) {
        if (fos != null) fos.write((ra + "\n").getBytes)
        ra = queue.poll
      }

      val currentTime = System.currentTimeMillis
      val dif = (currentTime - time) / 1000
      if (dif >= 10) {
        time = currentTime
        val writtenResources = written(writers)

        val rate = (writtenResources - totalWritten).asInstanceOf[Float] / (dif)
        val avgRate = (writtenResources).asInstanceOf[Float] / ((time - startTime) / 1000)

        totalWritten = writtenResources

        println("Written " + writtenResources + " resources (" + rate + ", " + avgRate + ") errors: " + errors(writers))
      }
    }

    var ra = queue.poll
    while (ra != null) {
      if (fos != null) fos.write((ra + "\n").getBytes)
      ra = queue.poll
    }


    val endTime = System.currentTimeMillis
    println(count + " objects written in " + (endTime - startTime) / 1000)

    if (fos != null) fos.close()
    executor.shutdown()

  }

  def isRunning(writers: List[ResourceWriter]): Boolean = writers.exists(!_.done)

  def written(writers: List[ResourceWriter]): Int = writers.map(e => e.written).foldLeft(0)(_ + _)

  def errors(writers: List[ResourceWriter]): Int = writers.map(e => e.errors).foldLeft(0)(_ + _)
}