package org.aphreet.c3.platform.client.access

import org.springframework.remoting.RemoteLookupFailureException

import org.aphreet.c3.platform.client.common._
import org.aphreet.c3.platform.client.common.ArgumentType._

import scala.collection.jcl.HashMap
import java.util.Random

import org.aphreet.c3.platform.remote.api.rmi.access.PlatformRmiAccessService

class PlatformAccessClient(override val args:Array[String]) extends CLI(args) with SpringRmiAccessor{
  
  def cliDescription = parameters(
    "size" has mandatory argument "num" described "Size of object to write",
    "count" has mandatory argument "num" described "Count of objects to write",
    "theads" has mandatory argument "num" described "Thead count",
    "type" has mandatory argument "mime" described "Mime type of content",
    "pool" has mandatory argument "name" described "Target pool",
    "help" described "Prints this message"
  )
   
  def getAccessService:PlatformRmiAccessService =
    obtainRmiService("rmi://localhost:1299/PlatformRmiAccessEndPoint", 
                     classOf[PlatformRmiAccessService])
  
  
  def main{
    if(cli.getOptions.length == 0) helpAndExit("Writer")

    if(cli.hasOption("help")) helpAndExit("Writer" )
    
    val objectSize = cliValue("size", "512").toInt
    val objectCount = cliValue("count", "-1").toInt
    val threadCount = cliValue("threads", "1").toInt
    val objectType = cliValue("type", "application/octet-stream")
    val pool = cliValue("pool", "")
    
    
    
    if(objectCount < 0)
      throw new IllegalArgumentException("Object count is not set")
    
    writeObjects(objectCount, objectSize, 1, Map("pool" -> pool, "content.type" -> objectType))
  }
  
  def writeObjects(count:Int, size:Int, threads:Int, metadata:Map[String, String]){
    
    var accessService:PlatformRmiAccessService = null
    
    try{
      accessService = getAccessService
    }catch{
      case e:RemoteLookupFailureException => {
        println("Failed to connect to C3")
        println("Error is: " + e.getMessage)
        System.exit(1)
      }
    }
    
    println("Writing " + count + " objects of size " + size)
    
    val startTime = System.currentTimeMillis
    var time = startTime;
   
    val md = (new HashMap[String, String]() ++ metadata).asInstanceOf[HashMap[String, String]].underlying
    
    for(i <- 1 to count){
      val ra = accessService.add(md, generateDataOfSize(size))
      if(i % 1000 == 0){
        val wtime = System.currentTimeMillis
        val rate = 1000f/((wtime - time)/1000f)
        println("Saved " + i + " objects with average rate " + rate + "(obj/s)")
        time = System.currentTimeMillis
      }
    }
    
    val endTime = System.currentTimeMillis
    
    println(count + " objects written in " + (endTime - startTime)/1000)
  }
  
  def generateDataOfSize(size:Int):Array[Byte] = {
    
    val result = new Array[Byte](size)
    val random = new Random(System.currentTimeMillis)
    random.nextBytes(result)
    
    result
  }
}
