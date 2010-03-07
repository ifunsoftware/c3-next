package org.aphreet.c3.platform.storage.volume

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import javax.annotation.{PostConstruct, PreDestroy}

import org.apache.commons.logging.LogFactory

import org.aphreet.c3.platform.task._
import org.aphreet.c3.platform.management.{SPlatformPropertyListener, PropertyChangeEvent}

import dataprovider.VolumeDataProvider
import org.aphreet.c3.platform.storage.Storage
import org.aphreet.c3.platform.exception.StorageException

@Component
class VolumeManager extends SPlatformPropertyListener{

  val WATERMARKS = "c3.platform.capacity.watermarks"
  
  val logger = LogFactory getLog getClass
  
  val dataProvider = VolumeDataProvider.getProvider
 
  val volumes:List[Volume] = loadVolumeData
  
  var taskManager:TaskManager = null
  
  @Autowired
  def setTaskManager(manager:TaskManager) = {taskManager = manager}
  
  @PostConstruct
  def init{
    taskManager.submitTask(new VolumeUpdater(volumes, dataProvider))
  }
  
  def register(storage:Storage) = {
	val volume = volumeForPath(storage.path.toString)
	
	if(volume != null){
		volume.storages + storage
		storage.volume = volume
	}else
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
  
  def listeningForProperties:Array[String] =
    Array(WATERMARKS)
  
  def defaultValues:Map[String,String] = 
    Map(WATERMARKS -> "50000000,100000000")
  
  def propertyChanged(event:PropertyChangeEvent) = {
    
    val newWatermarks:Array[Long] = event.newValue.split(",").map(_.toLong)
    
    val lowWatermark = newWatermarks(0)
    val highWatermark = newWatermarks(1)
    
    if(lowWatermark >= 0 && highWatermark>= 0 && lowWatermark <= highWatermark){
      
      logger info "Updating volume watermark"
      
      for(volume <- volumes){
        volume.setLowWatermark(lowWatermark)
        volume.setHighWatermark(highWatermark)
      }
      
    }else throw new StorageException("Failed to update volume watermarks, wrong values")
    
  }
  
  class VolumeUpdater(val volumes:List[Volume], dataProvider:VolumeDataProvider) extends Task {
    
    override def step{
      log.debug("updating volume state")
      
      val newVolumeList = dataProvider.getVolumeList
      
      for(newVolume <- newVolumeList)
        for(volume <- volumes)
          if(volume.mountPoint == newVolume.mountPoint)
            volume.updateState(newVolume.size, newVolume.available)
          
      
      log.debug(volumes.toString)
      Thread.sleep(10000)
    }
    
    override def name = "VolumeCapacityMonitor"
  }
  
}