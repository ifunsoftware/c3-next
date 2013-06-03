/*
 * Copyright (c) 2013, Mikhail Malygin
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
 * 3. Neither the name of the iFunSoftware nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES LOSS OF USE, DATA, OR PROFITS
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.aphreet.c3.platform.search.impl.index

import org.apache.lucene.document.{Document, Field}
import org.aphreet.c3.platform.resource.Metadata
import org.aphreet.c3.platform.search.impl.SearchConfiguration

class WeightedDocumentBuilder(val configuration: SearchConfiguration) {

  val EXTRACTED_SUFFIX = "_ex"

  val blackListedMeta = Set("domain", "c3.address", "lang")

  def build(metadata: Metadata, extractedMetadata: Map[String, String],
    content: String,
    language: String, address: String, domain: String): Document = {

    val weights = configuration.getFieldWeights

    val document = new Document()

    if(content != null){
      document.add(new Field("content", content, Field.Store.YES, Field.Index.ANALYZED))
    }

    // store extracted Metadata
    for (key <- extractedMetadata.keys) {
      if (!key.equalsIgnoreCase("domain")) {
        val field = new Field(key, extractedMetadata.get(key).get, Field.Store.YES, Field.Index.ANALYZED)

        if (weights.containsField(key)) {
          field.setBoost(weights.getBoostFactor(key, 1))
        }
        document.add(field)
      }
    }

    // store user metadata
    for (key <- metadata.asMap.keys) {
      if (!blackListedMeta.contains(key)) {

        val isAnalyzed = if(key.startsWith("__")){
          Field.Index.NOT_ANALYZED
        }else{
          Field.Index.ANALYZED
        }

        for(value <- metadata.collectionValue(key)){

          val field = new Field(key, value, Field.Store.YES, isAnalyzed)

          if (weights.containsField(key)) {
            field.setBoost(weights.getBoostFactor(key, 1))
          }

          document.add(field)
        }
      }
    }

    if(language != null){
      document.add(new Field("lang", language, Field.Store.YES, Field.Index.NOT_ANALYZED))
    }

    document.add(new Field("c3.address", address, Field.Store.YES, Field.Index.NOT_ANALYZED))
    document.add(new Field("domain", domain, Field.Store.YES, Field.Index.NOT_ANALYZED))

    document
  }
}
