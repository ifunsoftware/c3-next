package org.aphreet.c3.platform.test.unit

import junit.framework.TestCase
import junit.framework.Assert._
import org.aphreet.c3.platform.resource._

class ResourceAddressTestCase extends TestCase{

  def testParseAddress(){
    val address = "rZ1L9jbMHZgqCvT8gNk3u5iC-139e8b70f47-12341234"

    val parsed = ResourceAddress(address)

    assertEquals(ResourceAddress("12341234","rZ1L9jbMHZgqCvT8gNk3u5iC",1348229074759l), parsed)
  }

  def testSerializeAddress(){
    val address = ResourceAddress("12341234","rZ1L9jbMHZgqCvT8gNk3u5iC",1348229074759l)

    assertEquals("rZ1L9jbMHZgqCvT8gNk3u5iC-139e8b70f47-12341234", address.stringValue)
  }

  def testGenerateAddress(){
    val resource = new Resource
    val version = new ResourceVersion
    version.data = DataStream.create("Sample data")
    resource.addVersion(version)

    val address = ResourceAddress.generate(resource, "12341234")

    assertEquals(AddressGenerator.RESOURCE_ADDRESS_LENGTH, address.stringValue.length)
  }
}
