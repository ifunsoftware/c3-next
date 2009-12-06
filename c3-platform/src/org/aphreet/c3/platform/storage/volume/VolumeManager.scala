package org.aphreet.c3.platform.storage.volume

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import javax.annotation.{PostConstruct, PreDestroy}

import org.apache.commons.logging.LogFactory

import org.aphreet.c3.platform.management.PlatformPropertyListener

@Component
class VolumeManager extends PlatformPropertyListener{

  val LOW_WATERMARK = "c3.platform.capacity.lowwatermark"
  val HIGH_WATERMARK = "c3.platform.capacity.highwatermark"
  
  val logger = LogFactory getLog getClass
  
  val dataProvider = dataProviderForCurrentPlatform 
 
  val volumes:List[Volume] = loadVolumeData
  
  val executor = Executors.newSingleThreadScheduledExecutor
  
  def dataProviderForCurrentPlatform:VolumeDataProvider = {
    if(System.getProperty("os.name").startsWith("Windows")){
      new WinVolumeDataProvider
    }else{
      new LinuxVolumeDataProvider
    }
  }
  
  def register(storage:Storage) = {
	val volume = volumeForPath(storage.path.toString)
	
	if(volume != null)
		volume.storages + storage
	else
		throw new StorageException("Can't find volume for path: " + storage.path.toString)
  }
  
  def unregister(storage:Storage) = {
	val volume = volumeForPath(storage.path.toString)
	if(volume != null)
		volume.storages - storage
	else
		throw new StorageException("Can't find volume for path: " + storage.path.toString)
  }
  
  private def volumeForPath(path:String):Volume = {
    
    var foundVolume:Volume = null
    for(volume <- volumes)
      if(path contains volume.mountPoint)
        if(foundVolume == null)
          foundVolume = volume
        else
          if(foundVolume.mountPoint.length < volume.mountPoint.length)
        	foundVolume = volume
        
    foundVolume
    
  }
  
  @PostConstruct
  def init = {
	executor.scheduleAtFixedRate(new VolumeUpdater(volumes, dataProvider), 10, 10, TimeUnit.SECONDS)
  }
  
  @PreDestroy
  def destroy = {
    executor.shutdown
  }
  
  def loadVolumeData:List[Volume] = {
    val data = dataProvider.getVolumeList
    
    logger info data.toString
    
    data
  }
  
  def listeningForProperties:Array[String] = {
    Array(LOW_WATERMARK, HIGH_WATERMARK)
  }
  
  def propertyChanged(propName:String, oldValue:String, newValue:String) = {
    propName match {
      case LOW_WATERMARK => {
        logger info "Updating low watermark"
        for(volume <- volumes)
          volume.setLowWatermark(newValue.toLong)
      }
      case HIGH_WATERMARK => {
        logger info "Updating high watermark"
        for(volume <- volumes)
          volume.setHighWatermark(newValue.toLong)
      }
    }
  }
  
  class VolumeUpdater(val volumes:List[Volume], dataProvider:VolumeDataProvider) extends Runnable {
    
    def run = {
      
      logger.debug("updating volume state")
      
      val newVolumeList = dataProvider.getVolumeList
      
      for(newVolume <- newVolumeList)
        for(volume <- volumes)
          if(volume.mountPoint == newVolume.mountPoint){
            volume.updateState(newVolume.size, newVolume.available)
          }
      
      logger.debug(volumes.toString)
    }
  }
  
}