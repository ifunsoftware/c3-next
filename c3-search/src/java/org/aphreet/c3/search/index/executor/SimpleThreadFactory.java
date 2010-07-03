package org.aphreet.c3.search.index.executor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.aphreet.c3.search.index.ResourceHandler;
import org.aphreet.c3.search.index.event.DirectoryEventListener;
import org.aphreet.c3.search.index.event.RegisterDirectoryEventListener;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class SimpleThreadFactory implements ThreadFactory, RegisterDirectoryEventListener{

	static final AtomicInteger poolNumber = new AtomicInteger(1);
	protected final ThreadGroup group;
	protected final AtomicInteger threadNumber = new AtomicInteger(1);
	protected final String namePrefix;
	protected List<DirectoryEventListener> listeners;
	protected ResourceHandler resourceHandler;

	public SimpleThreadFactory() {
		SecurityManager s = System.getSecurityManager();
		group = (s != null)? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
		namePrefix = poolNumber.getAndIncrement() + "-thread-";
		listeners = new ArrayList<DirectoryEventListener>();
	}

	/**
	 * Returns Thread with new Runnable task r.
	 */
	public abstract Thread newThread(Runnable runnable);

	public synchronized void addDirectoryEventListener(DirectoryEventListener listener) {
		listeners.add(listener);
	}

	public synchronized void removeDirectoryEventListener(DirectoryEventListener listener) {
		listeners.remove(listener);
	}
	
	public List<DirectoryEventListener> getListeners() {
		return listeners;
	}

	@Autowired
	public void setResourceHandler(ResourceHandler resourceHandler) {
		this.resourceHandler = resourceHandler;
	}
	
}
