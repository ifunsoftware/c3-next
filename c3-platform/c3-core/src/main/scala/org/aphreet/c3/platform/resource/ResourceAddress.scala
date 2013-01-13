package org.aphreet.c3.platform.resource

case class ResourceAddress(systemId:String, randomPart:String, time:Long) {

  lazy val stringValue = randomPart + "-" +  time.toHexString + "-" + systemId

}

object ResourceAddress{

  val RESOURCE_ADDRESS_LENGTH = 41

  def apply(address:String):ResourceAddress = {

    val parts = address.split("-", 3)

    if(parts.size < 3){
      throw new ResourceException("Incorrect resource address format: " + address)
    }

    ResourceAddress(parts(2), parts(0), java.lang.Long.parseLong(parts(1), 16))
  }

  def generate(resource:Resource, systemId:String):ResourceAddress = {

    val idPart = IdGenerator.encodeBytes(resource.resourceHash)

    val time = System.currentTimeMillis()

    ResourceAddress(systemId, idPart, time)
  }

  def isValidAddress(address:String):Boolean = {
    if(address.length == RESOURCE_ADDRESS_LENGTH){
      try{
        ResourceAddress(address)
        true
      }catch{
        case e:ResourceException => false
      }
    }else false
  }

}
