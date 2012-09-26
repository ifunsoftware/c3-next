package org.aphreet.c3.platform.resource

import java.lang

case class ResourceAddress(systemId:String, randomPart:String, time:Long) {

  lazy val stringValue = randomPart + "-" +  time.toHexString + "-" + systemId

}

object ResourceAddress{

  def apply(address:String):ResourceAddress = {
    val parts = address.split("-", 3)

    ResourceAddress(parts(2), parts(0), java.lang.Long.parseLong(parts(1), 16))
  }

  def generate(resource:Resource, systemId:String):ResourceAddress = {

    val idPart = IdGenerator.encodeBytes(resource.versions.head.data.byteHash)

    val time = System.currentTimeMillis()

    ResourceAddress(systemId, idPart, time)
  }

}
