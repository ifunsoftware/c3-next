package org.aphreet.c3.platform.storage

import scala.collection.mutable.Set

trait StorageFactory {
  
  def storages:Set[Storage]
  
  def createStorage(params:StorageParams, systemId:String):Storage
  
  def name:String
  
}