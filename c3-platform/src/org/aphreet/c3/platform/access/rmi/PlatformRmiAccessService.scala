package org.aphreet.c3.platform.access.rmi

import java.util.HashMap

trait PlatformRmiAccessService {

  def add(metadata:HashMap[String, String], file:String):String
  
  def get(ra:String):Array[Byte]
  
  def getMetadata(ra:String):HashMap[String, String]
  
}
