package org.aphreet.c3.platform.storage

import org.aphreet.c3.platform.resource.{Resource, DataWrapper}

import java.io.OutputStream

trait ResourceAccessor {
  
  def get(ra:String):Resource
  
  def add(resource:Resource):String
  
  def update(resource:Resource):String
  
  def delete(ra:String)
}
