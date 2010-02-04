package org.aphreet.c3.platform.client.access

import org.springframework.remoting.RemoteLookupFailureException
import org.springframework.remoting.rmi.RmiProxyFactoryBean

import org.aphreet.c3.platform.remote.rmi.management.PlatformRmiManagementService
import org.aphreet.c3.platform.remote.rmi.access.PlatformRmiAccessService

import org.aphreet.c3.platform.client.common.CLI
import org.aphreet.c3.platform.client.common.ArgumentType._

import java.io.File
import java.util.HashMap
import java.util.Random

import org.apache.commons.cli._

class PlatformAccessClient(override val args:Array[String]) extends CLI(args){
  
  def cliDescription = cl_with_parameters(
    "size" has optional argument "num" described "Size of object to write",
    "count" has mandatory argument "num" described "Count of objects to write",
    "theads" has optional argument "num" described "Thead count",
    "help" described "Prints this message"
  )
   
  def getAccessService:PlatformRmiAccessService = {
    val rmiAccess = new RmiProxyFactoryBean
    rmiAccess.setServiceUrl("rmi://localhost:1299/PlatformRmiAccessEndPoint")
    rmiAccess.setServiceInterface(classOf[PlatformRmiAccessService])
    rmiAccess.afterPropertiesSet
    
    rmiAccess.getObject.asInstanceOf[PlatformRmiAccessService]
  }
  
  
  def main{
    if(cli.getOptions.length == 0) helpAndExit("Writer")

    if(cli.hasOption("help")) helpAndExit("Writer" )
    
    val objectSize = cliValue("size", "512").toInt
    val objectCount = cliValue("count", "-1").toInt
    
    if(objectCount < 0)
      throw new IllegalArgumentException("Object count is not set")
    
    writeObjects(objectCount, objectSize, 1, "")
  }
  
  def writeObjects(count:Int, size:Int, threads:Int, pool:String){
    
    var accessService:PlatformRmiAccessService = null
    
    try{
      accessService = getAccessService
    }catch{
      case e:RemoteLookupFailureException => {
        println("Failed to connect to C3 Platform")
        println("Error is: " + e.getMessage)
        System.exit(1)
      }
    }
    
    println("Writing " + count + " objects of size " + size)
    
    val startTime = System.currentTimeMillis
    var time = startTime;
    
    for(i <- 1 to count){
      val ra = accessService.add(new HashMap[String, String], generateDataOfSize(size))
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
