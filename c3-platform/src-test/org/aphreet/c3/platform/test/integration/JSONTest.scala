package org.aphreet.c3.platform.test.integration

import org.aphreet.c3.platform.common.JSONFormatter

import junit.framework.TestCase


class JSONTest extends TestCase{

  def testSerialization{
    
    val source = """{"firstName":"Иван","lastName":"Иванов","address":{"streetAddress":"Московское ш., 101, кв.101","city":"Ленинград","postalCode":101101},"phoneNumbers":["812 123-1234","916 123-4567"]}"""
    
    println(JSONFormatter.format(source))
    
  }
}
