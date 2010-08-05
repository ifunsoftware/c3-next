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

package org.aphreet.c3.platform.storage.volume.impl

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import javax.annotation.{PostConstruct}

import org.apache.commons.logging.LogFactory

import org.aphreet.c3.platform.task._
import org.aphreet.c3.platform.management.{SPlatformPropertyListener, PropertyChangeEvent}

import org.aphreet.c3.platform.storage.Storage
import org.aphreet.c3.platform.exception.StorageException
import org.aphreet.c3.platform.storage.volume.dataprovider.VolumeDataProvider
import org.aphreet.c3.platform.storage.volume.{VolumeManager, Volume}

@Component
class VolumeManagerImpl extends VolumeManager with SPlatformPropertyListener{

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

  def volumeList:List[Volume] = volumes

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
      log.trace("Updating volume state")

      val newVolumeList = dataProvider.getVolumeList

      for(newVolume <- newVolumeList)
        for(volume <- volumes)
          if(volume.mountPoint == newVolume.mountPoint)
            volume.updateState(newVolume.size, newVolume.available)


      log.trace(volumes.toString)
      Thread.sleep(10000)
    }

    override def name = "VolumeCapacityMonitor"
  }

}