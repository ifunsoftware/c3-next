package org.aphreet.c3.platform.storage.volume

import org.aphreet.c3.platform.storage.Storage

trait VolumeManager{

  def register(storage:Storage)
  
  def unregister(storage:Storage)
  
}