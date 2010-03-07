package org.aphreet.c3.platform.storage.volume.dataprovider

import java.io.{File, FileInputStream}
import java.util.Scanner

import java.io.{BufferedReader, InputStreamReader}
import org.aphreet.c3.platform.storage.volume.Volume
import org.aphreet.c3.platform.exception.StorageException

abstract class AbstractUnixDataProvider(val dfCommand:String) extends VolumeDataProvider{

  def getVolumeList:List[Volume] = {

    var result:List[Volume] = List()

    //"df -B 1"
    val process = Runtime.getRuntime.exec(dfCommand)
    
    process.waitFor
    
    if(process.exitValue != 0){
      throw new StorageException("Failed to get volume data, exit value " + process.exitValue)
    }
    
    val reader = new BufferedReader(new InputStreamReader(process.getInputStream))
    
    reader.readLine
    
    try{
    
    	var line = reader.readLine
      
	    while(line != null){
	      
	      val array = line.split("\\s+", 6)
      
	      val size = toBytes(array(1).toLong)
	      val avail = toBytes(array(3).toLong)
	      val mounted:String = array(5)
	      
       
	      result = new Volume(mounted, size, avail) :: result 
	      
          line = reader.readLine
	    }
	    
	    result

    }finally reader.close 
  }
  
  def toBytes(size:Long):Long = size
}
