/**
 * Copyright (c) 2010, Mikhail Malygin
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

package org.aphreet.c3.platform.remote.rest.serialization

import com.thoughtworks.xstream.XStream
import org.aphreet.c3.platform.resource.{Resource, ResourceVersion}
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver
import com.thoughtworks.xstream.io.xml.DomDriver
import com.thoughtworks.xstream.converters.extended.ISO8601DateConverter
import org.aphreet.c3.platform.search.{SearchResultFragment, SearchResultElement}
import org.aphreet.c3.platform.filesystem.NodeRef
import org.aphreet.c3.platform.remote.rest.response._
import fs.{FSNode, FSDirectory}

class XStreamFactory{

  def createXMLStream:XStream = {
    configureXMLStream(configureXStream(new XStream(new DomDriver("UTF-8"))))
  }

  def createJSONStream:XStream = {
    configureJSONStream(configureXStream(new XStream(new JettisonMappedXmlDriver)))
  }

  private def configureXStream(xStream:XStream):XStream = {
    xStream.registerConverter(new HashMapConverter)
    xStream.registerConverter(new ArrayBufferConverter(xStream.getMapper))
    xStream.registerConverter(new ISO8601DateConverter)

    xStream.setMode(XStream.NO_REFERENCES)

    xStream.aliasSystemAttribute(null, "class")

    xStream.alias("resource", classOf[Resource])
    xStream.alias("version", classOf[ResourceVersion])
    xStream.alias("p:response", classOf[Result])
    xStream.alias("p:response", classOf[ErrorResult])
    xStream.alias("p:response", classOf[ResourceResult])
    xStream.alias("p:response", classOf[SearchResult])
    xStream.alias("p:response", classOf[UploadResult])
    xStream.alias("p:response", classOf[DirectoryResult])
    xStream.alias("entry", classOf[SearchResultElement])
    xStream.alias("info", classOf[ResultInfo])

    xStream.alias("node", classOf[FSNode])

    xStream.alias("fragment", classOf[SearchResultFragment])

    xStream.aliasField("uploaded", classOf[UploadResult], "address")
    xStream.aliasField("trackVersions", classOf[Resource], "isVersioned")
    xStream.aliasField("error", classOf[ErrorResult], "errorDescription")

    xStream.omitField(classOf[Resource], "embedData")
    xStream.omitField(classOf[ResourceVersion], "data")
    xStream.omitField(classOf[ResourceVersion], "revision")
    xStream.omitField(classOf[ResourceVersion], "persisted")
    xStream
  }

  private def configureXMLStream(xStream:XStream):XStream = {

    xStream.useAttributeFor(classOf[Result], "namespace")
    xStream.useAttributeFor(classOf[Result], "schemeLocation")
    xStream.useAttributeFor(classOf[Result], "xsiScheme")
    xStream.useAttributeFor(classOf[ResultInfo], "version")
    xStream.useAttributeFor(classOf[ResultInfo], "status")
    xStream.useAttributeFor(classOf[ResourceAddress], "address")
    xStream.useAttributeFor(classOf[ResourceAddress], "version")

    xStream.useAttributeFor(classOf[FSNode], "address")
    xStream.useAttributeFor(classOf[FSNode], "leaf")
    xStream.useAttributeFor(classOf[FSNode], "name")

    xStream.useAttributeFor(classOf[FSDirectory], "name")
    xStream.useAttributeFor(classOf[FSDirectory], "address")



    xStream.aliasField("xmlns:p", classOf[Result], "namespace")
    xStream.aliasField("xsi:schemaLocation", classOf[Result], "schemeLocation")
    xStream.aliasField("xmlns:xsi", classOf[Result], "xsiScheme")

    xStream.useAttributeFor(classOf[SearchResultElement], "address")
    xStream.useAttributeFor(classOf[SearchResultElement], "score")
    xStream.useAttributeFor(classOf[SearchResultElement], "path")
    xStream.useAttributeFor(classOf[SearchResultFragment], "field")


    xStream.useAttributeFor(classOf[Resource], "address")
    xStream.useAttributeFor(classOf[Resource], "createDate")

    xStream.useAttributeFor(classOf[Resource], "isVersioned")

    xStream.useAttributeFor(classOf[ResourceVersion], "date")

    xStream
  }

  private def configureJSONStream(xStream:XStream):XStream = {
    xStream.omitField(classOf[Result], "namespace")
    xStream.omitField(classOf[Result], "schemeLocation")
    xStream.omitField(classOf[Result], "xsiScheme")

    xStream
  }
}