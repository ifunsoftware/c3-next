package org.aphreet.c3.platform.storage.volume

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import javax.annotation.{PostConstruct, PreDestroy}

import org.apache.commons.logging.LogFactory

import org.aphreet.c3.platform.task._
import org.aphreet.c3.platform.management.{PlatformPropertyListener, PropertyChangeEvent}

@Component
class VolumeManager extends PlatformPropertyListener{

  val LOW_WATERMARK = "c3.platform.capacity.lowwatermark"
  val HIGH_WATERMARK = "c3.platform.capacity.highwatermark"
  
  val logger = LogFactory getLog getClass
  
  val dataProvider = dataProviderForCurrentPlatform 
 
  val volumes:List[Volume] = loadVolumeData
  
  var taskExecutor:TaskExecutor = null
  
  @Autowired
  def setTaskExecutor(executor:TaskExecutor) = {taskExecutor = executor}
  
  @PostConstruct
  def init{
    taskExecutor.submitTask(new VolumeUpdater(volumes, dataProvider))
  }
  
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
  
  def loadVolumeData:List[Volume] = {
    val data = dataProvider.getVolumeList
    
    logger info data.toString
    
    data
  }
  
  def listeningForProperties:Array[String] = {
    Array(LOW_WATERMARK, HIGH_WATERMARK)
  }
  
  def propertyChanged(event:PropertyChangeEvent) = {
    event.name match {
      case LOW_WATERMARK => {
        logger info "Updating low watermark"
        for(volume <- volumes)
          volume.setLowWatermark(event.newValue.toLong)
      }
      case HIGH_WATERMARK => {
        logger info "Updating high watermark"
        for(volume <- volumes)
          volume.setHighWatermark(event.newValue.toLong)
      }
    }
  }
  
  class VolumeUpdater(val volumes:List[Volume], dataProvider:VolumeDataProvider) extends Task {
    
    def runExecution = {
      while(!Thread.currentThread.isInterrupted){
        if(!isPaused){
          logger.debug("updating volume state")
      
          val newVolumeList = dataProvider.getVolumeList
      
          for(newVolume <- newVolumeList)
            for(volume <- volumes)
              if(volume.mountPoint == newVolume.mountPoint){
                volume.updateState(newVolume.size, newVolume.available)
              }
      
          logger.debug(volumes.toString)
          Thread.sleep(10000)
        }
      }
      logger info (name + " ended")
      
    }
    
    def name = "VolumeCapacityMonitor"
    
    def progress = -1
    
  }
  
}