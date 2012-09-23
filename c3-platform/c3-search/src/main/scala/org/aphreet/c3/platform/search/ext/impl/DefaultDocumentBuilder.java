/**
 * Copyright (c) 2010, Mikhail Malygin
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * <p/>
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the IFMO nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * <p/>
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
package org.aphreet.c3.platform.search.ext.impl;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.aphreet.c3.platform.search.ext.DocumentBuilder;

import java.util.Map;


public class DefaultDocumentBuilder implements DocumentBuilder{

    @Override
    public Document build(Map<String, String> metadata, Map<String, String> extractedMetadata, String language, String domain) {

        Document doc = new Document();

        for(String key : extractedMetadata.keySet()){
            doc.add(new Field(key, extractedMetadata.get(key), Field.Store.COMPRESS, Field.Index.ANALYZED));
        }

        for(String key : metadata.keySet()){
            doc.add(new Field(key, metadata.get(key), Field.Store.COMPRESS, Field.Index.ANALYZED));
        }

        doc.add(new Field("domain", domain, Field.Store.YES, Field.Index.NOT_ANALYZED));

        return doc;
    }
}
