package org.aphreet.c3.search.index.executor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IndexThreadPoolExecutor extends ThreadPoolExecutor {
	
	private AtomicBoolean commitIndexes; 
	private final Log log = LogFactory.getLog(getClass());
	
	public IndexThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
			TimeUnit unit, ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>()
				, threadFactory);
		this.commitIndexes = new AtomicBoolean(false);
	}
	
	@Override
	protected void afterExecute(Runnable runnable, Throwable throwable) {
		super.afterExecute(runnable, throwable);
		log.info("IndexThreadPoolExecutor after execute " + runnable);
		ResourceIndexingTask task = (ResourceIndexingTask) runnable;
		RAMIndexThread ramIndexThread = (RAMIndexThread) Thread.currentThread();
		//if (throwable == null) {
			ramIndexThread.indexedResources().add(task.resourceAddress());
			ramIndexThread.checkBufferOverflow();
		//}
		if (commitIndexes.compareAndSet(true, false)) {
			this.shutdown();
			DirectoriedThreadFactory threadFactory = (DirectoriedThreadFactory) getThreadFactory();
			threadFactory.commitIndex();
//			try {
//				this.awaitTermination(15, TimeUnit.SECONDS);
//			} catch (InterruptedException e) {
//				Thread.currentThread().interrupt();
//			}
		}
	}
	
	public void setCommitIndex(boolean commit) {
		this.commitIndexes.set(commit);
	}

}
