package org.aphreet.c3.search.impl;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.misc.TrigramLanguageGuesser;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.aphreet.c3.search.config.SearchConfig;
import org.aphreet.c3.search.index.ResourceHandler;
import org.aphreet.c3.search.index.event.DirectoryEvent;
import org.aphreet.c3.search.index.event.DirectoryEventListener;
import org.aphreet.c3.search.index.filter.LanguageGuesserFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class C3MultipleIndexSearcher implements DirectoryEventListener {
	
	private Log logger = LogFactory.getLog(this.getClass());
	
	public static final String[] fieldsToSearchIn = {
		ResourceHandler.COMMENT, ResourceHandler.CONTENTS, ResourceHandler.TAGS,
		ResourceHandler.AUTHOR, ResourceHandler.CREATOR, ResourceHandler.TITLE, 
		ResourceHandler.SUBJECT, ResourceHandler.KEYWORDS, ResourceHandler.DESCRIPTION};

	private Set<IndexSearcher> indexSearchers;
	@Autowired
	private LanguageGuesserFilter languageGuesserFilter;
	
	public C3MultipleIndexSearcher() {
		indexSearchers = new HashSet<IndexSearcher>();
	}

	@Override
	public void directoryAdded(DirectoryEvent event) {
		try {
			IndexSearcher indexSearcher = new IndexSearcher(event.getDirectory());
			synchronized (indexSearchers) {
				indexSearchers.add(indexSearcher);
			}
		} catch (IOException e) {
			logger.error(e, e);
		}
	}

	@Override
	public void directoryDeleted(DirectoryEvent event) {
		try {
			for (IndexSearcher searcher : indexSearchers) {
				if (searcher.getIndexReader().directory().equals(event.getDirectory())) {
					synchronized (indexSearchers) {
						indexSearchers.remove(searcher);
						searcher.close();
					}
				}
			}
		} catch (IOException e) {
			logger.error(e, e);
		}
	}

	@Override
	public void directoryUpdated(DirectoryEvent event) {
		try {
			for (IndexSearcher searcher : indexSearchers) {
				if (searcher.getIndexReader().directory().equals(event.getDirectory())) {
					synchronized (indexSearchers) {
						searcher.getIndexReader().reopen();
					}
				}
			}
		} catch (IOException e) {
			logger.error(e, e);
		}
	}

	public List<String> searchResource(String queryString) throws IOException {
        List<String> foundResourceAddresses = new ArrayList<String>();
        String language = languageGuesserFilter.guessLanguage(queryString);
        logger.debug("Language of query " + queryString + " is " + language);

        QueryParser parser = null;
        Query query = null;
        try {
            if (ResourceHandler.RUSSIAN_LANGUAGE.equalsIgnoreCase(language)){
                parser = new MultiFieldQueryParser(fieldsToSearchIn,
                                new RussianAnalyzer());
            }else{
                parser = new MultiFieldQueryParser(fieldsToSearchIn,
                                new StandardAnalyzer());
            }
            query = parser.parse(queryString);
            synchronized (indexSearchers) {
            	for (IndexSearcher searcher : indexSearchers) {
                TopDocs docs = searcher.search(query, 10000);
	                for (ScoreDoc scoreDoc : docs.scoreDocs) {
	                    Document d = searcher.doc(scoreDoc.doc);
	                    foundResourceAddresses.add(d.getField(ResourceHandler.ADDRESS)
	                    		+ ":" + d.getField(ResourceHandler.REVISION));
	                }
            	}
			}
        } catch (Exception e) {
            logger.error(this, e);
        }
        return foundResourceAddresses;
	}

	public void setLanguageGuesserFilter(LanguageGuesserFilter languageGuesserFilter) {
		this.languageGuesserFilter = languageGuesserFilter;
	}

}
