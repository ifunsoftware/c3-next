
/**
 * Copyright (c) 2011, Mikhail Malygin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *

 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.aphreet.c3.platform.remote.test.serialization.xml

import junit.framework.TestCase
import org.aphreet.c3.platform.remote.rest.serialization.XStreamFactory
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.InputSource
import java.io.{StringReader, FileInputStream, File}
import javax.xml.validation.SchemaFactory
import org.aphreet.c3.platform.remote.rest.response._
import org.aphreet.c3.platform.resource.{DataWrapper, Resource, ResourceVersion}
import org.aphreet.c3.platform.search.SearchResultEntry

class TestXmlSerialization extends TestCase{

  def testError{

    val e = new Exception("test")

    val xStream = new XStreamFactory().createXMLStream

    val output = xStream.toXML(new ErrorResult(new ErrorDescription("error message here", e)))

    val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + output

    verifyXml(xml)
  }

  def testResource{

    val resource = new Resource
    resource.address = "my_resource_address"
    resource.metadata.put("md_key", "sdsd")
    resource.metadata.put("md_key2", "md\"value2")

    val version = new ResourceVersion
    version.data = DataWrapper.wrap("string")
    version.systemMetadata.put("sys_key", "sys_value")

    resource.addVersion(version)

    val xStream = new XStreamFactory().createXMLStream

    val output = xStream.toXML(new ResourceResult(resource))

    val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + output

    verifyXml(xml)

  }

  def testSearch{
    val entry = new SearchResultEntry("address", 0.12f, Array("sdfdf", "sdfsdf", "sdfdsfdsfsdf"))

    val result = Array(entry)

    val xStream = new XStreamFactory().createXMLStream

    val output = xStream.toXML(new SearchResult(result))

    val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + output

    println(xml)

    verifyXml(xml)
  }

  def testUpload{

    val xStream = new XStreamFactory().createXMLStream

    val output = xStream.toXML(new UploadResult(new ResourceAddress("sfdsfdsfdsf", 1)))

    val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + output

    verifyXml(xml)

  }

  def verifyXml(xml:String){

    var file = new File("c3-remote/WebContent/rest_1_0.xsd")

    if(!file.exists){
      file = new File("WebContent/rest_1_0.xsd")
    }

    val factory = SAXParserFactory.newInstance

    factory.setValidating(false)
    factory.setNamespaceAware(true)

    val schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema")
    factory.setSchema(schemaFactory.newSchema(Array[Source](new StreamSource(new FileInputStream(file)))))

    val parser = factory.newSAXParser

    val reader = parser.getXMLReader
    reader.setErrorHandler(new JunitErrorHandler)
    reader.parse(new InputSource(new StringReader(xml)))

  }
}