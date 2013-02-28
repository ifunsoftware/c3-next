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
package org.aphreet.c3.platform.search.impl.index

import org.aphreet.c3.platform.resource.{Metadata, Resource}
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.aphreet.c3.platform.search.impl.common.Fields
import org.aphreet.c3.platform.search.ext.{SearchConfiguration, DocumentBuilderFactory}
import collection.JavaConversions._
import org.apache.lucene.document.{Document, Field}
import org.apache.lucene.util.Version
import org.aphreet.c3.platform.search.impl.index.extractor.ExtractedDocument
import java.util.Collections


class ResourceHandler(val factory: DocumentBuilderFactory,
                      val searchConfiguration: SearchConfiguration,
                      val resource: Resource,
                      val metadata: Metadata,
                      val extracted: Option[ExtractedDocument],
                      val lang: String){


  def document:Document = {

    val documentBuilder = factory.createDocumentBuilder(searchConfiguration)

    val domain = resource.systemMetadata.get("c3.domain.id").get

    extracted match {
      case Some(document) => documentBuilder.build(mapAsJavaMap(metadata.asMap), mapAsJavaMap(document.metadata),
        document.content, lang, resource.address, domain)
      case None => documentBuilder.build(mapAsJavaMap(metadata.asMap), Collections.emptyMap(), null, lang, resource.address, domain)
    }

  }

  def analyzer:Analyzer = {
    
    if(lang == "ru") new RussianAnalyzer(Version.LUCENE_35)
    else new StandardAnalyzer(Version.LUCENE_35)

  }
}