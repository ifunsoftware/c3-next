package org.aphreet.c3.platform.storage

import java.io.OutputStream

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
  
  def iterator:StorageIterator
  
  def close;
  
 
  def path:String
  
  def name:String
}
