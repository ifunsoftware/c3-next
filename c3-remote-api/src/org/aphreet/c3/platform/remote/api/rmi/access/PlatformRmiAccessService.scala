package org.aphreet.c3.platform.remote.api.rmi.access

import java.util.HashMap
import org.aphreet.c3.platform.remote.api.access.PlatformAccessService

trait PlatformRmiAccessService extends PlatformAccessService{

  def add(metadata:HashMap[String, String], data:Array[Byte]):String

}
