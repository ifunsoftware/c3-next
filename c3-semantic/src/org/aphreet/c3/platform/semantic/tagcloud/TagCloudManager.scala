package org.aphreet.c3.platform.semantic.tagcloud

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Apr 3, 2010
 * Time: 10:21:34 AM
 * To change this template use File | Settings | File Templates.
 */

import java.util.Map
import org.aphreet.c3.platform.resource.Resource

trait TagCloudManager{

  def cloudForPool(pool:String):Map[String, Int]

  def resourceAdded(resource:Resource)

  def resourceUpdated(oldResource:Resource, newResource:Resource)

  def resourceDeleted(resource:Resource)
}