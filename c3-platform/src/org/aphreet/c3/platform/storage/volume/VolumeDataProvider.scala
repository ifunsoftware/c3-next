package org.aphreet.c3.platform.storage.volume


trait VolumeDataProvider {

  def getVolumeList:List[Volume]
  
}
