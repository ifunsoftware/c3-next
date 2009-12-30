package org.aphreet.c3.platform.storage

import java.io.OutputStream

import org.aphreet.c3.platform.common.Path
import org.aphreet.c3.platform.resource.{Resource, DataWrapper}


trait Storage {
  
  def id:String;
  
  def ids:List[String];

  
  def add(resource:Resource):String
  
  def get(ra:String):Option[Resource]

  def update(resource:Resource):String
  
  def delete(ra:String)
  
  
  def params:StorageParams
  
  def mode:StorageMode
  
  def mode_=(mode:StorageMode)
  
  def count:Long
  
  def iterator:StorageIterator
  
  def close;
  
 
  def path:Path
  
  def name:String
}
