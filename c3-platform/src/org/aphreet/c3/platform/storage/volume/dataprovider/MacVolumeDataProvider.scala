package org.aphreet.c3.platform.storage.volume.dataprovider

class MacVolumeDataProvider extends AbstractUnixDataProvider("df -lb") {

  override def toBytes(size:Long):Long = size * 512
  
}
