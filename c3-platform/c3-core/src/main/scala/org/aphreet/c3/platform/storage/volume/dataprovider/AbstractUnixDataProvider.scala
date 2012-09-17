/**
 * Copyright (c) 2010, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.aphreet.c3.platform.storage.volume.dataprovider

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

        if(array.length >= 6){
          val size = toBytes(array(1).toLong)
          val avail = toBytes(array(3).toLong)
          val mounted:String = array(5)


          result = new Volume(mounted, size, avail) :: result
        }
	      
        line = reader.readLine
	    }
	    
	    result

    }finally reader.close()
  }
  
  def toBytes(size:Long):Long = size
}