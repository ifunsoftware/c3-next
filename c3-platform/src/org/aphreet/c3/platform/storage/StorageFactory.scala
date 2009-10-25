package org.aphreet.c3.platform.storage

import scala.collection.Set

trait StorageFactory {
  
  def storages:Set[Storage]
  
  def createStorage(params:StorageParams):Storage
  
  def name:String
  
}
