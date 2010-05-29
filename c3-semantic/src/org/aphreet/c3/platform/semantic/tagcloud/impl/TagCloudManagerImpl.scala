package org.aphreet.c3.platform.semantic.tagcloud.impl

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Apr 3, 2010
 * Time: 10:21:58 AM
 * To change this template use File | Settings | File Templates.
 */

import java.util.Map

import org.aphreet.c3.platform.resource.Resource
import com.sleepycat.je.Database
import org.aphreet.c3.platform.semantic.tagcloud.TagCloudManager

class TagCloudManagerImpl extends TagCloudManager{

  var database:Database = null

  def cloudForPool(pool:String):Map[String, Int] = {
    null
  }

  def resourceAdded(resource:Resource){

  }

  def resourceUpdated(oldResource:Resource, newResource:Resource){

  }

  def resourceDeleted(resource:Resource){
    
  }

}