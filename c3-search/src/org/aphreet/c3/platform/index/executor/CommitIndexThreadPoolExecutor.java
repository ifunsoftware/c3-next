package org.aphreet.c3.search.index.executor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class CommitIndexThreadPoolExecutor extends ThreadPoolExecutor {

	private final Log log = LogFactory.getLog(getClass());

	public CommitIndexThreadPoolExecutor(CommitIndexThreadFactory threadFactory) {
		super(1, 1, 0L, TimeUnit.MICROSECONDS, new LinkedBlockingQueue<Runnable>(), threadFactory);
	}
	
	@Override
	protected void beforeExecute(Thread thread, Runnable runnable) {
		super.beforeExecute(thread, runnable);
		log.info("CommitIndexThreadPoolExecutor before execute"); 
		CommitIndexThread commitIndexThread = (CommitIndexThread) Thread.currentThread();
		CommitIndexTask task = (CommitIndexTask) runnable;
		commitIndexThread.notifyListenersThatDirectoryDeleted(task.getPersistentDirectory());
	}
	
	@Override
	protected void afterExecute(Runnable runnable, Throwable throwable) {
		super.afterExecute(runnable, throwable);
		log.info("CommitIndexThreadPoolExecutor after execute"); 
		CommitIndexThread commitIndexThread = (CommitIndexThread) Thread.currentThread();
		CommitIndexTask task = (CommitIndexTask) runnable;
		commitIndexThread.notifyListenersThatDirectoryUpdated(task.getPersistentDirectory());
	}
	
}
