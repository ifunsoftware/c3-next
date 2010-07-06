package org.aphreet.c3.search.index.executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.search.index.C3IndexWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommitIndexThreadFactory extends SimpleThreadFactory {
	
	private Log logger = LogFactory.getLog(this.getClass());
	private C3IndexWriter persistentIndexWriter;

	/**
	 * Returns Thread with new Runnable task r.
	 */
	public Thread newThread(Runnable runnable) {
		Thread thread = null;
			try {
				thread = new CommitIndexThread(persistentIndexWriter, getListeners());
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
	
}
