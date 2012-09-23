package org.aphreet.c3.platform.search.ext;

import org.apache.lucene.search.Searcher;

public interface SearchStrategy {

    SearchResultEntry[] search(Searcher searcher, String query, int max, int offset, String domain);

}
