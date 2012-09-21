package org.aphreet.c3.platform.resource

case class ResourceAddress(systemId:String, randomPart:String, time:Long) {

  lazy val stringValue = randomPart + "-" + time.toHexString + "-" + systemId

}

object ResourceAddress{

  def apply(address:String):ResourceAddress = {
    val parts = address.split("-", 3)

    ResourceAddress(parts(2), parts(0), java.lang.Long.parseLong(parts(1), 16))
  }

  def generate(systemId:String):ResourceAddress = {
    val randomPart = IdGenerator.generateId(6)
    val time = System.currentTimeMillis()

    ResourceAddress(systemId, randomPart, time)
  }

}
