package org.aphreet.c3.platform.search.ext.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CachingTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.aphreet.c3.platform.search.ext.FieldWeights;
import org.aphreet.c3.platform.search.ext.SearchConfiguration;
import org.aphreet.c3.platform.search.ext.SearchResultEntry;
import org.aphreet.c3.platform.search.ext.SearchStrategy;

import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Ildar
 * Date: 02.02.2011
 * Time: 0:24:01
 */
public class MultiFieldSearchStrategy  implements SearchStrategy {

    private static final Log log = LogFactory.getLog(MultiFieldSearchStrategy.class);

    private SearchConfiguration configuration;

    public MultiFieldSearchStrategy(SearchConfiguration configuration){
        this.configuration = configuration;
    }

    @Override
    public SearchResultEntry[] search(Searcher searcher, String query, int max, int offset, String domain) {
        Analyzer analyzer = new StandardAnalyzer();
        Analyzer russianAnalyzer = new RussianAnalyzer();
        FieldWeights fieldWeights = configuration.getFieldWeights();

        try {
            MultiFieldQueryParser parser = new MultiFieldQueryParser(fieldWeights.getFields(), analyzer);
            query += " AND domain:" + domain; // only docs from current domain
            Query searchQuery = parser.parse(query);

            Set termsSet = new HashSet();
            searchQuery.extractTerms(termsSet);
            Set<String> fieldsToSearchIn = new HashSet<String>();
            for (Object o : termsSet) {
                String field = ((Term) o).field();
                if (!field.equalsIgnoreCase("domain")) {
                    fieldsToSearchIn.add(field);
                }
            }

            TopDocs topDocs = searcher.search(searchQuery, max);

            ArrayList<SearchResultEntry> result = new ArrayList<SearchResultEntry>();

            for(ScoreDoc doc : topDocs.scoreDocs) {

                float score = doc.score;
                int num = doc.doc;
                Document document = searcher.doc(num);
                String address = document.get("c3.address");
                String language = document.get("lang");

                Fieldable field;
                Map<String, String[]> fieldFragments = new HashMap<String, String[]>();
                for (Object o : document.getFields()) {
                    field = (Fieldable) o;
                    if (fieldsToSearchIn.contains(field.name())) {
                        String content = document.get(field.name());
                        String[] fragments = fragmentsWithHighlightedTerms(
                                language.equals("ru") ? russianAnalyzer : analyzer, searchQuery, field.name(),
                                content, 5, 100);
                        if (fragments.length > 0) {
                            fieldFragments.put(field.name(), fragments);
                        }
                    }
                }

                result.add(new SearchResultEntry(address, score, fieldFragments));

            }

            return result.toArray(new SearchResultEntry[result.size()]);

        } catch (Exception e) {
            log.error(e, e);
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
