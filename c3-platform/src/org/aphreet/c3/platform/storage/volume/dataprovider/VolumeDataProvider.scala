package org.aphreet.c3.platform.storage.volume.dataprovider

import org.aphreet.c3.platform.storage.volume.Volume

trait VolumeDataProvider {

  def getVolumeList:List[Volume]
  
}

object VolumeDataProvider {
  
  def getProvider:VolumeDataProvider = {
    
    val osName = System.getProperty("os.name");
    
    if(osName.startsWith("Windows")){
      new WinVolumeDataProvider
    }else if(osName.startsWith("Mac")){
      new MacVolumeDataProvider
    }else
      new LinuxVolumeDataProvider 
   }
}
