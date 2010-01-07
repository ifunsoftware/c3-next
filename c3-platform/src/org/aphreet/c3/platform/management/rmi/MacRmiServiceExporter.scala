package org.aphreet.c3.platform.management.rmi

import java.rmi.RemoteException
import java.rmi.registry.{Registry, LocateRegistry}

import org.springframework.remoting.rmi.RmiServiceExporter

class MacRmiServiceExporter extends RmiServiceExporter{

  override def getRegistry(registryPort:Int):Registry = {
    
    if (logger.isInfoEnabled) 
	  logger.info("Looking for RMI registry at port '" + registryPort + "'")
	
    try {
	  // Retrieve existing registry.
	  val reg = LocateRegistry.getRegistry("127.0.0.1", registryPort)
	  testRegistry(reg)
	  reg
	}catch{
      case e:RemoteException => {
		logger.debug("RMI registry access threw exception", e)
	    logger.info("Could not detect RMI registry - creating new one")
	    // Assume no registry found -> create new one.
		LocateRegistry.createRegistry(registryPort)
        }
    }
  }
}
