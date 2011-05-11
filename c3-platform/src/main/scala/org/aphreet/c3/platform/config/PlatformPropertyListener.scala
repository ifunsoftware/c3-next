package org.aphreet.c3.platform.config

import java.util.{Map, Collections}

trait PlatformPropertyListener {

  def listeningForProperties:Array[String]
  
  def propertyChanged(event:PropertyChangeEvent)
  
  def defaultPropertyValues:Map[String, String]
  
}
