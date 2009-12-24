package org.aphreet.c3.search.index.executor;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.aphreet.c3.platform.access.PlatformAccessEndpoint;
import org.aphreet.c3.platform.resource.Resource;
import org.aphreet.c3.search.config.SearchConfig;
import org.aphreet.c3.search.index.ResourceHandler;

public class ResourceIndexingTask implements Runnable {
	
	private final Log logger = LogFactory.getLog(getClass()); 

	private Resource resource;
	private IndexWriter indexWriter;
	private ResourceHandler resourceHandler;
	
	public ResourceIndexingTask(Resource resource) {
		this.resource = resource;
	}
	
	public void run() {
		RAMIndexThread ramIndexThread = (RAMIndexThread) Thread.currentThread();
		indexWriter = ramIndexThread.getIndexWriter();
		resourceHandler = ramIndexThread.getResourceHandler();
		if (indexWriter == null)
			throw new NullPointerException("indexWriter can't be null.");
		index(resource);
	}
	
	public String resourceAddress() {
		return resource.address();
	}

	protected void index(Resource resource) {
		logger.info("Begin indexing resource " + this.resource.address());
		Document doc = resourceHandler.process(resource);
		try {
			String language = resource.getMetadata().get(ResourceHandler.LANGUAGE).toLowerCase();
			// choose correct analyzer to process indexing
			if (language.matches(".*" + ResourceHandler.RUSSIAN_LANGUAGE + ".*")) {
				indexWriter.addDocument(doc, new RussianAnalyzer());
			} else {
				indexWriter.addDocument(doc);
			}
			logger.info("Resource " + resource.address() + " added to index");
		} catch (Exception e) {
			logger.error(getClass(), e);
		} 
	}
	
}
