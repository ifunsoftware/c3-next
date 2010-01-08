package org.aphreet.c3.platform.test.unit

import org.aphreet.c3.platform.common.JSONFormatter

import junit.framework.TestCase
import junit.framework.Assert._

class JSONFormatting extends TestCase{

  def testFormatting{
    
    val source = """{"firstName":"John","lastName":"Smith","address":{"streetAddress":"2d Street","city":"NY","postalCode":101101},"phoneNumbers":["812 123-1234","916 123-4567"]}"""
    
    val expected = 
"""{
	"firstName": "John",
	"lastName": "Smith",
	"address": {
		"streetAddress": "2d Street",
		"city": "NY",
		"postalCode": 101101
	},
	"phoneNumbers": [
		"812 123-1234",
		"916 123-4567"
	]
}"""
    assertEquals(expected, JSONFormatter.format(source))
  }
}
