package org.aphreet.c3.platform.storage.volume

import java.io.{File, FileInputStream}
import java.util.Scanner

class LinuxVolumeDataProvider extends VolumeDataProvider{

  def getVolumeList:List[Volume] = {

    var result:List[Volume] = List()

    val scanner = new Scanner(Runtime.getRuntime.exec("df -B 1 | grep '[0-9][0-9]*%'").getInputStream)

    try{
    
	    while(scanner.hasNext){
	      val device:String = scanner.next
	      val size = scanner.nextLong
	      val used = scanner.nextLong
	      val avail = scanner.nextLong
	      val percentage:String = scanner.next
	      val mounted:String = scanner.next
	      
	      result = new Volume(mounted, size, avail) :: result 
	    }
	    
	    result

    }finally
    	scanner.close
    
    
  }
}