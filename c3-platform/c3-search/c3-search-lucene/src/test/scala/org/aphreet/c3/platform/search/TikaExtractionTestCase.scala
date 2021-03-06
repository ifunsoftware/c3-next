package org.aphreet.c3.platform.search

import junit.framework.TestCase
import org.aphreet.c3.platform.resource.DataStream
import java.io.File
import org.aphreet.c3.platform.search.lucene.impl.index.extractor.TikaHttpTextExtractor

abstract class TikaExtractionTestCase extends TestCase{

  def testTextExtraction(){

    val client = new TikaHttpTextExtractor("http://tika-ifunsoftware.rhcloud.com")

    println(client.callTika("address", DataStream.create(new File("/emc/malygm/Downloads/2.4_Console_FUNSpec.doc")), "application/octet-stream"))

  }
}
