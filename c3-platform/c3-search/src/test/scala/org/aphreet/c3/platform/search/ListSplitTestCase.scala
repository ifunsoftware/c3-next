package org.aphreet.c3.platform.search

import junit.framework.TestCase
import junit.framework.Assert._
import org.aphreet.c3.platform.search.ext.impl.WeightedDocumentBuilder
import scala.collection.JavaConversions

class ListSplitTestCase extends TestCase{

  def testListSplit(){

    verifyListParse(List("value1", "value2", "value3"), "value1,value2,value3")
    verifyListParse(List("value1", "value2", "", "value3"), "value1,value2,,value3")
    verifyListParse(List("value1", "value2", "", "value3",""), "value1,value2,,value3,")
    verifyListParse(List("", "value1", "value2", "", "value3",""), ",value1,value2,,value3,")
    verifyListParse(List("", "", "value1", "value2", "", "value3",""), ",,value1,value2,,value3,")

    verifyListParse(List("value1,value2", "value3"), "value1\\,value2,value3")

    verifyListParse(List("value1", "value2", "\\,value3"), "value1,value2,\\,value3")


  }

  def verifyListParse(expected: List[String], value: String){

    val result = WeightedDocumentBuilder.splitList(value)

    assertEquals(expected, JavaConversions.collectionAsScalaIterable(result).toList)
  }
}
