package org.aphreet.c3.platform.management

trait PlatformPropertyListener {

  def listeningForProperties:Array[String]
  
  def propertyChanged(event:PropertyChangeEvent)
}
