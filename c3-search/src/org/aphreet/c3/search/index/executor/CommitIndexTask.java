package org.aphreet.c3.search.index.executor;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.aphreet.c3.platform.access.PlatformAccessEndpoint;
import org.aphreet.c3.platform.resource.Resource;
import org.aphreet.c3.search.config.SearchConfig;

public class CommitIndexTask implements Runnable {
	
	private final Log logger = LogFactory.getLog(getClass());
	private RAMDirectory directory;
	private Set<String> indexedResources;
	private PlatformAccessEndpoint platformAccessEndpoint;
	private IndexWriter presistentIndexWriter;
	
	public CommitIndexTask(RAMDirectory directory, Set<String> indexedResources,
			PlatformAccessEndpoint endpoint) {
		this.directory = directory;
		this.indexedResources = indexedResources;
		this.platformAccessEndpoint = endpoint;
	}

	@Override
	public void run() {
		logger.info("Begin commiting index with resources " + indexedResources);
		CommitIndexThread thread = (CommitIndexThread) Thread.currentThread();
		presistentIndexWriter = thread.getPersistentIndexWriter();
		Resource resource = null;
		try {
			presistentIndexWriter.addIndexesNoOptimize(new Directory [] {directory});
			logger.info("Index commited. Mark resources as indexed.");
			for (String address : indexedResources) {
				resource = platformAccessEndpoint.get(address);
				resource.getSysMetadata().put(SearchConfig.INDEXED, Boolean.TRUE.toString());
				logger.info("Mark resource " + address + " as indexed");
			}
		} catch (IOException e) {
			logger.error(e, e);
		}
	}

	public Directory getPersistentDirectory() {
		return this.presistentIndexWriter.getDirectory();
	}

}
