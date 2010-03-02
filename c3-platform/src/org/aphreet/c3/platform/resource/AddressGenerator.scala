package org.aphreet.c3.platform.resource

import java.util.UUID

/**
 * Created by IntelliJ IDEA.
 * User: Aphreet
 * Date: Mar 3, 2010
 * Time: 12:39:42 AM
 * To change this template use File | Settings | File Templates.
 */

object AddressGenerator {

  val RESOURCE_ADDRESS_LENGTH = 41

  def addressForStorage(id:String):String = {
    UUID.randomUUID.toString + "-" + id
  }

  def storageForAddress(address:String):String = {
    if(isValidAddress(address))
      address.substring(address.length - 4, address.length)
    else
      ""
  }

  def isValidAddress(address:String):Boolean = {
     address.length == RESOURCE_ADDRESS_LENGTH
  }
}