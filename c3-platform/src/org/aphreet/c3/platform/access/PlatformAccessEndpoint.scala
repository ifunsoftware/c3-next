package org.aphreet.c3.platform.access

import java.io.OutputStream
import java.util.List

import org.aphreet.c3.platform.resource.{Resource, DataWrapper}

trait PlatformAccessEndpoint {

  def get(ra:String):Resource
  
  def add(resource:Resource):String
  
  def update(resource:Resource):String
  
  def delete(ra:String)
  
  def search(query:String):List[String]
  
  def query(query:String):List[String]
}
