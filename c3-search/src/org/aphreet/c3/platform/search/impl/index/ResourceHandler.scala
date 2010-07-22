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

import filter.ResourceFilter
import org.aphreet.c3.platform.resource.Resource
import collection.mutable.HashMap
import org.apache.lucene.document.{Field, Document}
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.ru.RussianAnalyzer
import org.aphreet.c3.platform.search.impl.common.Fields


class ResourceHandler(val resource:Resource, val filters:List[ResourceFilter]){

  val fields = new HashMap[String, String]

  {
    for(filter <- filters if filter.support(resource)){
      fields ++ filter.apply(resource, fields)
    }

  }

  def document:Document = {

    val doc = new Document

    fields.removeKey(Fields.ADDRESS)

    doc.add(new Field(Fields.ADDRESS, resource.address, Field.Store.YES, Field.Index.NOT_ANALYZED))

    fields.foreach(e => doc.add(new Field(e._1, e._2, Field.Store.YES, Field.Index.ANALYZED)))

    doc
  }

  def analyzer:Analyzer = {
    fields.get(Fields.LANG) match {
      case Some(lang) =>
        if(lang == "ru") new RussianAnalyzer
        else new StandardAnalyzer
      case None => new StandardAnalyzer
    }
  }
}