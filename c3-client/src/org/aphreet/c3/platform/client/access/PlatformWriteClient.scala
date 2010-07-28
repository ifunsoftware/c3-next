package org.aphreet.c3.platform.client.access

import org.aphreet.c3.platform.client.common.CLI
import org.aphreet.c3.platform.client.common.ArgumentType._
import java.util.concurrent.{Executors, LinkedBlockingQueue}
import java.io.{OutputStream, BufferedOutputStream, File, FileOutputStream}

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 6, 2010
 * Time: 11:09:00 PM
 * To change this template use File | Settings | File Templates.
 */

class PlatformWriteClient(override val args: Array[String]) extends CLI(args) {
  def cliDescription = parameters(
    "host" has mandatory argument "host" described "Host to connect to",
    "size" has mandatory argument "num" described "Size of object to write",
    "count" has mandatory argument "num" described "Count of objects to write",
    "threads" has mandatory argument "num" described "Thread count",
    "type" has mandatory argument "mime" described "Mime type of content",
    "pool" has mandatory argument "name" described "Target pool",
    "out" has mandatory argument "file" described "File to write resource addressed",
    "help" described "Prints this message"
    )

  def run {
    if (cli.getOptions.length == 0) helpAndExit("Writer")

    if (cli.hasOption("help")) helpAndExit("Writer")

    val objectSize = cliValue("size", "512").toInt
    val objectCount = cliValue("count", "-1").toInt
    val threadCount = cliValue("threads", "1").toInt
    val objectType = cliValue("type", "application/octet-stream")
    var pool = cliValue("pool", "")

    //    println(pool.getBytes.toString)
    //    println(pool.getBytes("UTF-8").toString)
    //    pool = new String(pool.getBytes("UTF-8"))
    //    println(pool)
    //This is kind of magic
    //This code works for UTF-8 locale
    //But i don't know what will happen on windows machines
    //Possibly
    pool = new String(pool.getBytes, "UTF-8")


    val host = "http://" + cliValue("host", "localhost:8080") + "/c3-remote/resource/"
    val file = cliValue("out", null)



    if (objectCount < 0)
      throw new IllegalArgumentException("Object count is not set")

    writeObjects(host, objectCount, objectSize, threadCount, Map("c3.pool" -> pool, "content.type" -> objectType), file)
  }

  def writeObjects(host: String, count: Int, size: Int, threads: Int, metadata: Map[String, String], file: String) {

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
      writers = new ResourceWriter(host, toWrite).size(size).metadata(metadata).queue(queue) :: writers
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

    if (fos != null) fos.close
    executor.shutdown

  }

  def isRunning(writers: List[ResourceWriter]): Boolean = writers.exists(!_.done)

  def written(writers: List[ResourceWriter]): Int = writers.map(e => e.written).foldLeft(0)(_ + _)

  def errors(writers: List[ResourceWriter]): Int = writers.map(e => e.errors).foldLeft(0)(_ + _)
}