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
	private Object synchronizationObject;
	private final Log log = LogFactory.getLog(getClass());
	
	public IndexThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
			TimeUnit unit, ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>()
				, threadFactory);
		this.commitIndexes = new AtomicBoolean(false);
		synchronizationObject = new Object();
	}

    @Override
    protected void beforeExecute(Thread thread, Runnable runnable) {
        IndexTask task = (IndexTask) runnable;
        IndexThread indexThread = (IndexThread) thread;
        task.setIndexWriter(indexThread.getIndexWriter());
        task.setResourceHandler(indexThread.getResourceHandler());
        super.beforeExecute(thread, runnable);
    }
	
	@Override
	protected void afterExecute(Runnable runnable, Throwable throwable) {
		super.afterExecute(runnable, throwable);
		IndexTask task = (IndexTask) runnable;
		IndexThread ramIndexThread = (IndexThread) Thread.currentThread();
		//if (throwable == null) {
			ramIndexThread.indexedResources().add(task.resourceAddress());
			ramIndexThread.checkBufferOverflow();
		//}
	}
	
	/**
	 * The calling method will be blocked until all index 
	 * buffers will not committed to regular store. 
	 */
	public void commitIndex() {
		shutdown();
		getQueue().clear();
		IndexThreadFactory threadFactory = (IndexThreadFactory) getThreadFactory();
		threadFactory.commitIndex();
//		commitIndexes.set(true);
//		synchronized(synchronizationObject) {
//			try {
//				synchronizationObject.wait();
//			} catch (InterruptedException e) {
//				Thread.currentThread().interrupt();
//				log.error(e);
//			}
//		}
	}

}
