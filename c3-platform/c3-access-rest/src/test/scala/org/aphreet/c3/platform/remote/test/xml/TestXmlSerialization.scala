package org.aphreet.c3.platform.remote.test.xml

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
import junit.framework.TestCase
import org.aphreet.c3.platform.remote.rest.serialization.XStreamFactory
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.InputSource
import java.io.{StringReader, FileInputStream, File}
import javax.xml.validation.SchemaFactory
import org.aphreet.c3.platform.remote.rest.response._
import org.aphreet.c3.platform.remote.rest.response.fs.{FSNodeData, FSNode, FSDirectory}
import org.aphreet.c3.platform.resource.{DataStream, Resource, ResourceVersion}
import org.aphreet.c3.platform.search.{SearchResultElement, SearchResultFragment}
import collection.mutable
import java.util.Date

class TestXmlSerialization extends TestCase{

  def testError(){

    val e = new Exception("test")

    val xStream = new XStreamFactory().createXMLStream

    val output = xStream.toXML(new ErrorResult(new ErrorDescription("error message here", e)))

    val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + output

    verifyXml(xml)
  }

  def testResource(){

    val resource = new Resource
    resource.address = "my_resource_address"
    resource.metadata("md_key") = "sdsd"
    resource.metadata("md_key2") = "md\"value2"

    val version = new ResourceVersion
    version.data = DataStream.create("string")
    version.date = new Date()
    version.systemMetadata("sys_key") = "sys_value"
    version.systemMetadata("c3.data.length") = "10240"
    version.systemMetadata("c3.data.md5") = "42688c7115664cc2a14c73ef9af6d266"

    resource.addVersion(version)

    val xStream = new XStreamFactory().createXMLStream

    val output = xStream.toXML(new ResourceResult(resource))

    val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + output

    verifyXml(xml)
  }

  def testSearch(){
    val entry = SearchResultElement("address", "/test", 0.12f,
      Array(SearchResultFragment("content", Array("qwe", "qweqwe", "qwewqe")),
        SearchResultFragment("name", Array("sdfsdf"))))

    val result = Array(entry)

    val xStream = new XStreamFactory().createXMLStream

    val output = xStream.toXML(new SearchResult("query", result))

    val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + output

    verifyXml(xml)
  }

  def testUpload(){

    val xStream = new XStreamFactory().createXMLStream

    val output = xStream.toXML(new UploadResult(new ResourceAddress("sfdsfdsfdsf", 1)))

    val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + output

    verifyXml(xml)

  }

  def testDirectory(){

    val xStream = new XStreamFactory().createXMLStream

    val metadata = new mutable.HashMap[String, String]
    metadata.put("key", "value")

    val directory = new FSDirectory("name", "address",
      Array(FSNode("name", "address", leaf = true, metadata, null),
        FSNode("name2", "address2", leaf = false, null, FSNodeData("<html>my string data value<p:response xmlns:p=\"http://c3.aphreet.org/rest/1.0\" xsi:schemaLocation=\"http://c3.aphreet.org/rest/1.0 http://c3-system.googlecode.com/files/rest.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><p:response xmlns:p=\"http://c3.aphreet.org/rest/1.0\" xsi:schemaLocation=\"http://c3.aphreet.org/rest/1.0 http://c3-system.googlecode.com/files/rest.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><html>my string data value<p:response xmlns:p=\"http://c3.aphreet.org/rest/1.0\" xsi:schemaLocation=\"http://c3.aphreet.org/rest/1.0 http://c3-system.googlecode.com/files/res".getBytes("UTF-8"), new Date))))

    val output = xStream.toXML(new DirectoryResult(directory))

    val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + output

    verifyXml(xml)
  }

  def testEmpty(){

    val xStream = new XStreamFactory().createXMLStream

    val output = xStream.toXML(new Result)

    val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + output

    verifyXml(xml)
  }

  def verifyXml(xml:String){

    var file = new File("c3-access-rest/src/main/support/rest_1_0.xsd")

    if(!file.exists){
      file = new File("src/main/support/rest_1_0.xsd")
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