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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CachingTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.aphreet.c3.platform.search.ext.SearchResultEntry;
import org.aphreet.c3.platform.search.ext.SearchStrategy;

import java.io.IOException;
import java.util.ArrayList;


public class DefaultSearchStrategy implements SearchStrategy{

    @Override
    public SearchResultEntry[] search(Searcher searcher, String query, int max, int offset, String domain) {

        Analyzer analyzer = new StandardAnalyzer();

        try {
            Query searchQuery = new QueryParser("content", analyzer).parse(query);

            TopDocs topDocs = searcher.search(searchQuery, max);

            ArrayList<SearchResultEntry> result = new ArrayList<SearchResultEntry>();

            for(ScoreDoc doc : topDocs.scoreDocs){

                float score = doc.score;
                int num = doc.doc;

                Document document = searcher.doc(num);

                String address = document.get("c3.address");
                String content = document.get("content");

                String[] fragments = fragmentsWithHighlightedTerms(analyzer, searchQuery, "content", content, 5, 100);

//                result.add(new SearchResultEntry(address, score, fragments));

            }

            return result.toArray(new SearchResultEntry[result.size()]);

        } catch (Exception e) {
            e.printStackTrace();
            return new SearchResultEntry[0];
        }
    }

    public String[] fragmentsWithHighlightedTerms(Analyzer analyzer, Query query,
                                                  String fieldName, String fieldContent,
                                                  int fragmentNumber, int fragmentSize) throws IOException {

        TokenStream stream = TokenSources.getTokenStream(fieldName, fieldContent, analyzer);
        SpanScorer scorer = new SpanScorer(query, fieldName, new CachingTokenFilter(stream));
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, fragmentSize);

        Highlighter highlighter = new Highlighter(scorer);

        highlighter.setTextFragmenter(fragmenter);
        highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);

        return highlighter.getBestFragments(stream, fieldContent, fragmentNumber);



    }
}
