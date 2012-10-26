package org.aphreet.c3.platform.search.ext;

import org.apache.lucene.search.IndexSearcher;

public interface SearchStrategy {

    SearchResultEntry[] search(IndexSearcher searcher, SearchConfiguration configuration,
                               String query, int max, int offset, String domain);

}
