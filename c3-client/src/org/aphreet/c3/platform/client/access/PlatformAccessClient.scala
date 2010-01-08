package org.aphreet.c3.platform.client.access

import org.springframework.remoting.rmi.RmiProxyFactoryBean

import org.aphreet.c3.platform.management.rmi.PlatformRmiManagementService
import org.aphreet.c3.platform.access.rmi.PlatformRmiAccessService

import java.io.File

import java.util.HashMap

import java.util.Random

object PlatformAccessClient {
  
  var accessService:PlatformRmiAccessService = null
  
  {
    val rmiAccess = new RmiProxyFactoryBean
    rmiAccess.setServiceUrl("rmi://localhost:1299/PlatformRmiAccessEndPoint")
    rmiAccess.setServiceInterface(classOf[PlatformRmiAccessService])
    rmiAccess.afterPropertiesSet
    
    accessService = rmiAccess.getObject.asInstanceOf[PlatformRmiAccessService]
  }
  
  
  def main(args : Array[String]) : Unit = {
    
    val objectCount = 50000;
    val objectSize = 512;
    
    for(i <- 1 to objectCount){
      val ra = accessService.add(new HashMap[String, String], generateDataOfSize(objectSize))
      if(i % 1000 == 0){
        println("Saved " + i + " objects")
      }
      //println("Saved data with ra: " + ra )
      
    }
  }
  
  def generateDataOfSize(size:Int):Array[Byte] = {
    
    val result = new Array[Byte](size)
    
    val random = new Random(System.currentTimeMillis)
    
    random.nextBytes(result)
    
    result
  }
}
