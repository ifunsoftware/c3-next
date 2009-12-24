package org.aphreet.c3.search.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.naming.OperationNotSupportedException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.aphreet.c3.platform.access.PlatformAccessEndpoint;
import org.aphreet.c3.platform.management.PlatformManagementEndpoint;
import org.aphreet.c3.platform.resource.Resource;
import org.aphreet.c3.platform.search.SearchManager;
import org.aphreet.c3.platform.search.Searcher;
import org.aphreet.c3.search.config.SearchConfig;
import org.aphreet.c3.search.index.C3IndexWriter;
import org.aphreet.c3.search.index.event.DirectoryEvent;
import org.aphreet.c3.search.index.executor.CommitIndexThreadFactory;
import org.aphreet.c3.search.index.executor.CommitIndexThreadPoolExecutor;
import org.aphreet.c3.search.index.executor.DirectoriedThreadFactory;
import org.aphreet.c3.search.index.executor.IndexThreadPoolExecutor;
import org.aphreet.c3.search.index.executor.ResourceIndexingTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SearcherImpl implements Searcher {
	
	private Log logger = LogFactory.getLog(this.getClass());
	@Autowired
	private PlatformAccessEndpoint platformAccessEndpoint;
	@Autowired
	private PlatformManagementEndpoint platformManagementEndpoint;
	@Autowired
	private SearchConfig searchConfig;
	@Autowired
	private C3MultipleIndexSearcher indexSearcher;
	@Autowired
	private SearchManager searchManager;
	@Autowired
	private DirectoriedThreadFactory directoriedThreadFactory;
	@Autowired
	private CommitIndexThreadFactory commitIndexThreadFactory;
	private C3IndexWriter persistentIndexWriter;
	private IndexThreadPoolExecutor indexingExecutor;

	private CommitIndexThreadPoolExecutor commitIndexExecutor;
	
	@PostConstruct
	public void init() throws IOException {
		searchManager.registerSearcher(this);
		// TODO annotate persistentIndexWriter with @Autowired
		// open persistent index store
		this.persistentIndexWriter = new C3IndexWriter(new File(searchConfig.getIndexDirectoryPath()),
				new StandardAnalyzer(), MaxFieldLength.UNLIMITED);
		// register searcher on writer
		this.persistentIndexWriter.registerSearcher(indexSearcher);

		// process actions needed to commit index in single thread pool executor
		this.commitIndexThreadFactory.addDirectoryEventListener(indexSearcher);
		this.commitIndexThreadFactory.setPersistentIndexWriter(persistentIndexWriter);
		this.commitIndexExecutor = new CommitIndexThreadPoolExecutor(commitIndexThreadFactory);
		
		// process actions needed to buffer index to RAM
		this.directoriedThreadFactory.addDirectoryEventListener(indexSearcher);
		this.directoriedThreadFactory.setCommitIndexExecutor(commitIndexExecutor);
		this.indexingExecutor = new IndexThreadPoolExecutor(SearchConfig.INITIAL_CORE_POOL_SIZE,
				SearchConfig.INITIAL_MAXIMUM_POOL_SIZE, SearchConfig.INITIAL_KEEP_ALIVE_TIME,
				TimeUnit.MINUTES, directoriedThreadFactory);
	}
	
	@PreDestroy
	public void destroy() {
		searchManager.unregisterSearcher(this);
		try {
			indexingExecutor.setCommitIndex(true);
			directoriedThreadFactory.commitIndex();
			persistentIndexWriter.close();
		} catch (IOException e) {
			logger.error(e, e);
		}
	}

	@Override
	public void index(Resource resource) {
		logger.info("Begin index resource " + resource.address());
		ResourceIndexingTask task = new ResourceIndexingTask(resource);
		this.indexingExecutor.execute(task);
	}
	
	@Override
	public List<String> search(String query) {
		logger.info("Begin search for query " + query);
		List<String> list = null;
		try {
			list = indexSearcher.searchResource(query);
		} catch (IOException e) {
			logger.error("", e);
		}
		return list;
	}

	public void setPlatformAccessEndpoint(PlatformAccessEndpoint platformAccessEndpoint) {
		this.platformAccessEndpoint = platformAccessEndpoint;
	}

	public void setPlatformManagementEndpoint(PlatformManagementEndpoint platformManagementEndpoint) {
		this.platformManagementEndpoint = platformManagementEndpoint;
	}

	public void setSearchConfig(SearchConfig searchConfig) {
		this.searchConfig = searchConfig;
	}
	
	public void setIndexSearcher(C3MultipleIndexSearcher indexSearcher) {
		this.indexSearcher = indexSearcher;
	}
	
	public void setSearchManager(SearchManager searchManager) {
		this.searchManager = searchManager;
	}
	
	public void setDirectoriedThreadFacory(DirectoriedThreadFactory threadFactory) {
		this.directoriedThreadFactory = threadFactory;
	}
	
	public void setCommitIndexThreadFacory(CommitIndexThreadFactory threadFactory) {
		this.commitIndexThreadFactory = threadFactory;
	}

}
