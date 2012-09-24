package org.aphreet.c3.platform.zone

case class Zone(storageIds:List[String])

object Zone{

  def apply(storageId:String):Zone = Zone(List(storageId))
}