package org.aphreet.c3.platform.resource

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 18, 2010
 * Time: 12:20:17 AM
 * To change this template use File | Settings | File Templates.
 */

class MissingResource(val reqAddress:String) extends Resource{

  {
    address = reqAddress;
    createDate = new java.util.Date(0)
    
  }

  override def addVersion(version:ResourceVersion){
    throw new UnsupportedOperationException
  }

  override def mimeType:String = "application/c3-missing"
  
}