package org.aphreet.c3.platform.metadata

import java.util.Map

trait MetadataService {

  def tagCloudForPool(poolName:String):Map[String, Int];
  
  def tagCloud:Map[String, Int];
  
}
