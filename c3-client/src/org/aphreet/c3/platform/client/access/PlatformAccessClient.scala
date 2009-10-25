package org.aphreet.c3.platform.client.access

import org.springframework.remoting.rmi.RmiProxyFactoryBean

import org.aphreet.c3.platform.management.rmi.PlatformRmiManagementService
import org.aphreet.c3.platform.access.rmi.PlatformRmiAccessService

import java.io.File

import java.util.HashMap

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
    
    for(fname <- args){
      val file = new File(fname)
      
      if(file.exists){
    	val ra = accessService.add(new HashMap[String, String], file.getAbsolutePath)
    	println("file " + fname + " saved with address " + ra)
      }else{
        println("file " + fname + " not exitst")
      }
      
      
    }
  }
}
