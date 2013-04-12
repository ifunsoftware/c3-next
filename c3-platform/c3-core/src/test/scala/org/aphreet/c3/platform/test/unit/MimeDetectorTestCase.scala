
package org.aphreet.c3.platform.test.unit

import eu.medsea.util.EncodingGuesser
import java.util.{HashSet, Collections}
import eu.medsea.mimeutil.{MimeType, TextMimeDetector, MimeUtil}
import junit.framework.{Assert, TestCase}
import scala.collection.JavaConversions
import eu.medsea.mimeutil.detector.MagicMimeMimeDetector
import org.aphreet.c3.platform.resource.DataStream
import java.io.File

class MimeDetectorTestCase extends TestCase {

  def testDetector() {

    val string = "this is my text!"

    val encodings = new HashSet[String]
    encodings.add("UTF-8")
    encodings.add("cp1251")
    encodings.add("US-ASCII")
    encodings.add("UTF-16")
    encodings.add("KOI8-R")

    EncodingGuesser.setSupportedEncodings(encodings)
    TextMimeDetector.setPreferredEncodings(Array("UTF-8"))

    MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector")

    val types = MimeUtil.getMimeTypes(string.getBytes("UTF-8"))

    val it = types.iterator

    while(it.hasNext){
      val mimeType:MimeType = it.next.asInstanceOf[MimeType]
      Assert.assertEquals("text/plain;charset=UTF-8", mimeType.toString)
    }
  }
}