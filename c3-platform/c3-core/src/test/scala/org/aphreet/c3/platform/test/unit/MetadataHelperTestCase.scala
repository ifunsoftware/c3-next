package org.aphreet.c3.platform.test.unit

import junit.framework.Assert._
import org.aphreet.c3.platform.resource.MetadataHelper
import junit.framework.TestCase

class MetadataHelperTestCase extends TestCase{

  def testListSplit(){

    verifyListParse(List(""), "[]")
    verifyListParse(List("[[["), "[[[")
    verifyListParse(List("value1", "value2", "value3"), "[value1,value2,value3]")
    verifyListParse(List("value1", "value2", "", "value3"), "[value1,value2,,value3]")
    verifyListParse(List("value1", "value2", "", "value3",""), "[value1,value2,,value3,]")
    verifyListParse(List("", "value1", "value2", "", "value3",""), "[,value1,value2,,value3,]")
    verifyListParse(List("", "", "value1", "value2", "", "value3",""), "[,,value1,value2,,value3,]")

    verifyListParse(List("value1,value2", "value3"), "[value1\\,value2,value3]")

    verifyListParse(List("value1", "value2", "\\,value3"), "[value1,value2,\\,value3]")
  }

  def verifyListParse(expected: List[String], value: String){
    assertEquals(expected, MetadataHelper.parseSequence(value).toList)
  }
}
