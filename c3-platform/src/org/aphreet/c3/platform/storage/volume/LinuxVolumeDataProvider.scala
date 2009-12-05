package org.aphreet.c3.platform.storage.volume

import java.io.{File, FileInputStream}
import java.util.Scanner

import java.io.{BufferedReader, InputStreamReader}

class LinuxVolumeDataProvider extends VolumeDataProvider{

  def getVolumeList:List[Volume] = {

    var result:List[Volume] = List()

    val process = Runtime.getRuntime.exec("df -B 1")
    
    if(process.exitValue != 0){
      throw new StorageException("Failed to get volume data, exit value " + process.exitValue)
    }
    
    val reader = new BufferedReader(new InputStreamReader(process.getInputStream))
    
    reader.readLine
    
    try{
    
    	var line = reader.readLine
      
	    while(line != null){
	      
	      val array = line.split("\\s+", 6)
      
	      val size = array(1).toLong
	      val avail = array(3).toLong
	      val mounted:String = array(5)
	      
       
	      result = new Volume(mounted, size, avail) :: result 
	      
          line = reader.readLine
	    }
	    
	    result

    }finally
    	reader.close
    
    
  }
}