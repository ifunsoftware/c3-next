package org.aphreet.c3.platform.search.ext;

import org.apache.lucene.document.Document;

import java.util.Map;

public interface DocumentBuilder {

    Document build(Map<String, String> metadata,
                   Map<String, String> extractedMetadata,
                   String contentReader,
                   String language,
                   String address,
                   String domain);
    
}
