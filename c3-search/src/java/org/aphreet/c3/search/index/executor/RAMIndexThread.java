package org.aphreet.c3.search.index.executor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.RAMDirectory;
import org.aphreet.c3.platform.access.AccessManager;
import org.aphreet.c3.search.config.SearchConfig;
import org.aphreet.c3.search.index.ResourceHandler;
import org.aphreet.c3.search.index.event.DirectoryEvent;
import org.aphreet.c3.search.index.event.DirectoryEventListener;

public class RAMIndexThread extends Thread {
	
	public static Long MAX_BUFFER_SIZE = 64000000L;
	private final Log logger = LogFactory.getLog(getClass());
	
	private RAMDirectory directory;
	private IndexWriter bufferIndexWriter;
	private List<DirectoryEventListener> listeners;
	private CommitIndexThreadPoolExecutor commitIndexExecutor;
	private ResourceHandler resourceHandler;
	private Set<String> indexedResources;
	private AccessManager platformAccessEndpoint;
	private DirectoryEventListener [] array;
	
	/**
	 * Creates new indexing buffer in RAM,
	 * notifies listeners about this.
	 * @param group the thread group
	 * @param r first task
	 * @param name the name of the thread 
	 * @param listeners list of <code>DirectoryEventListener</code>s
	 * 					that will be notified about buffer creation.
	 * @throws IOException if some error occurs during buffer creation 
	 */
	public RAMIndexThread(ThreadGroup group, Runnable r, String name,
			final List<DirectoryEventListener> listeners, CommitIndexThreadPoolExecutor executor,
			ResourceHandler resourceHandler, AccessManager endpoint) throws IOException {
		super(group, r, name);
		this.commitIndexExecutor = executor;
		this.resourceHandler = resourceHandler;
		this.platformAccessEndpoint = endpoint;
		this.listeners = listeners;
		createNewBuffer();
	}

	/**
	 * If buffer size more {@link SearchConfig#INITIAL_RAM_BUFFER_SIZE}
	 * interrupt current thread, give buffer reference to persistantIndexWriter.
	 */
	public boolean checkBufferOverflow() {
		boolean overflow = false;
		//if (directory.sizeInBytes() >= SearchConfig.INITIAL_RAM_BUFFER_SIZE) {
			// pass the directory to commitIndexExecutor
			commitBuffer();
			overflow = true;
			try {
				createNewBuffer(); 
			} catch (IOException e) {
				logger.error(e, e);
			}
		//} 
		return overflow;
	}
	
	public IndexWriter getIndexWriter() {
		return bufferIndexWriter;
	}

	public ResourceHandler getResourceHandler() {
		return resourceHandler;
	}
	
	public Set<String> indexedResources() {
		return indexedResources;
	}
	
	public void commitBuffer() {
		logger.info("commiting buffer with resources " + indexedResources);
		CommitIndexTask task = new CommitIndexTask(directory, indexedResources, platformAccessEndpoint);
		commitIndexExecutor.execute(task);
	}
	
	protected void createNewBuffer() throws IOException {
		logger.info("Creating new buffer");
		directory = new RAMDirectory();
		bufferIndexWriter = new IndexWriter(directory, new StandardAnalyzer(), true, MaxFieldLength.UNLIMITED);
		indexedResources = new HashSet<String>();
		// need to clone to avoid ConcurrentException
		logger.info("listeners=" + listeners);
		array = listeners.toArray(new DirectoryEventListener[listeners.size()]);
		new Thread(new Runnable() {
			public void run() {
				DirectoryEvent event = new DirectoryEvent(RAMIndexThread.this, directory);
				for (DirectoryEventListener listener : array) {
					listener.directoryAdded(event);
				}
			}
		}).start();
	}
}
