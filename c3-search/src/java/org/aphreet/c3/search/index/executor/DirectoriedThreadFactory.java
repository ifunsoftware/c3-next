package org.aphreet.c3.search.index.executor;

import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.platform.access.AccessManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Produces <code>Thread</code>s to index {@linkplain org.aphreet.c3.platform.resource.Resource}
 * using {@linkplain org.apache.lucene.store.RAMDirectory}
 * 
 * @author darik
 */
@Component
public class DirectoriedThreadFactory extends SimpleThreadFactory {

	private Log logger = LogFactory.getLog(this.getClass());
	private CommitIndexThreadPoolExecutor commitIndexThreadPoolExecutor;
	
	@Autowired
	private AccessManager platformAccessEndpoint;

	public Thread newThread(Runnable runnable) {
		Thread thread = null;
			try {
				thread = new RAMIndexThread(group, runnable, namePrefix + threadNumber.getAndIncrement(), 
						getListeners(), commitIndexThreadPoolExecutor, resourceHandler, platformAccessEndpoint);
			if (thread.isDaemon())
				thread.setDaemon(false);
			if (thread.getPriority() != Thread.NORM_PRIORITY)
				thread.setPriority(Thread.NORM_PRIORITY);
		} catch (Exception e) {
			logger.error("", e);
		}
		return thread;
	}

	public void setCommitIndexExecutor(CommitIndexThreadPoolExecutor executor) {
		this.commitIndexThreadPoolExecutor = executor;
	}

	public void setPlatformAccessEndpoint(AccessManager platformAccessEndpoint) {
		this.platformAccessEndpoint = platformAccessEndpoint;
	}
	
	/**
	 * This method must be called by one of the threads created 
	 * by this factory to interrupt all of them, as they are in one group.
	 */
	@SuppressWarnings("null")
	public void commitIndex() {
		Thread.currentThread().getThreadGroup().interrupt();
		Thread [] threads = null;
		Thread.currentThread().getThreadGroup().enumerate(threads);
		for (Thread t : threads) {
			if (t instanceof RAMIndexThread) {
				((RAMIndexThread)t).commitBuffer();
			} else {
				logger.info("This could not happen!!!!!!!!!!!!!!");
			}
		}
			
	}

}
