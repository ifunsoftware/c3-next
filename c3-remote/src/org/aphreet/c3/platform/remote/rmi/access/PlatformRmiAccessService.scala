package org.aphreet.c3.platform.remote.rmi.access

import java.util.HashMap

trait PlatformRmiAccessService {

  def add(metadata:HashMap[String, String], data:Array[Byte]):String
  
  def get(ra:String):Array[Byte]
  
  def getMetadata(ra:String):HashMap[String, String]
  
  def getResourceAsString(ra:String):String
}
