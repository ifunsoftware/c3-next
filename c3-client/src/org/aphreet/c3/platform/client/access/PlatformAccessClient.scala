package org.aphreet.c3.platform.client.access

import org.springframework.remoting.RemoteLookupFailureException
import org.springframework.remoting.rmi.RmiProxyFactoryBean

import org.aphreet.c3.platform.remote.rmi.management.PlatformRmiManagementService
import org.aphreet.c3.platform.remote.rmi.access.PlatformRmiAccessService

import java.io.File
import java.util.HashMap
import java.util.Random

import org.apache.commons.cli._

object PlatformAccessClient {
  
  def getAccessService:PlatformRmiAccessService = {
    val rmiAccess = new RmiProxyFactoryBean
    rmiAccess.setServiceUrl("rmi://localhost:1299/PlatformRmiAccessEndPoint")
    rmiAccess.setServiceInterface(classOf[PlatformRmiAccessService])
    rmiAccess.afterPropertiesSet
    
    rmiAccess.getObject.asInstanceOf[PlatformRmiAccessService]
  }
  
  
  def main(args : Array[String]) : Unit = {
    
    var objectCount = -1;
    var objectSize = 512;
    
    val cli = commandLine(args)
    
    if(cli.getOptions.length == 0){
      new HelpFormatter().printHelp("Writer", options)
      System.exit(0)
    }
    
    if(cli.hasOption("help")){
      new HelpFormatter().printHelp("Writer", options)
      System.exit(0)
    }
    
    if(cli.hasOption("size")) objectSize = Integer.parseInt(cli.getOptionValue("size"))
    
    if(cli.hasOption("count")) objectCount = Integer.parseInt(cli.getOptionValue("count"))
    
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
  
  def objectCountOption:Option = {
    val option = OptionBuilder.create("count" );
    option.setArgName("num")
    option.setDescription("Count of objects to write")
    option.setArgs(1)
    option.setOptionalArg(false)
    
    option
  }
  
  def objectSizeOption:Option = {
    val option = OptionBuilder.create("size")
    option.setArgName("num")
    option.setDescription("Size of object")
    option.setArgs(1)
    option.setOptionalArg(true)
    
    option
  }
  
  def helpOption:Option = {
    new Option("help", "prints this message")
  }
  
  def threadsOption:Option = {
    val option = OptionBuilder.create("threads")
    option.setArgName("num")
    option.setDescription("Number of threads")
    option.setArgs(1)
    option.setOptionalArg(true)
    
    option
  }
  
  def options:Options = {
    val options = new Options
    
    options addOption helpOption
    options addOption objectCountOption
    options addOption objectSizeOption
    options addOption threadsOption
  }
  
  def commandLine(args:Array[String]):CommandLine = {
    val parser = new PosixParser
    
    parser.parse(options, args)
  }
}
