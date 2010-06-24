package org.aphreet.c3.search.index.executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.search.index.C3IndexWriter;
import org.springframework.stereotype.Component;

@Component
public class CommitIndexThreadFactory extends SimpleThreadFactory {
	
	private static final ThreadGroup threadGroup = new ThreadGroup("commit-index-thread-group");
	private Log logger = LogFactory.getLog(this.getClass());
	private C3IndexWriter persistentIndexWriter;

	/**
	 * Returns Thread with new Runnable task r.
	 */
	public Thread newThread(Runnable runnable) {
		Thread thread = null;
			try {
				thread = new CommitIndexThread(threadGroup, runnable, namePrefix + "commit-index-thread-" + threadNumber.getAndIncrement(), 
                        persistentIndexWriter, getListeners());
			if (thread.isDaemon())
				thread.setDaemon(false);
			if (thread.getPriority() != Thread.NORM_PRIORITY)
				thread.setPriority(Thread.NORM_PRIORITY);
		} catch (Exception e) {
			logger.error("", e);
		}
		return thread;
	}

	public void setPersistentIndexWriter(C3IndexWriter indexWriter) {
		this.persistentIndexWriter = indexWriter;
	}

	public C3IndexWriter getPersistentIndexWriter() {
		return persistentIndexWriter;
	}
	
}
