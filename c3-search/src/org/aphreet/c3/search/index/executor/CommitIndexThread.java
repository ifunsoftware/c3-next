package org.aphreet.c3.search.index.executor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.store.Directory;
import org.aphreet.c3.platform.access.PlatformAccessEndpoint;
import org.aphreet.c3.search.index.C3IndexWriter;
import org.aphreet.c3.search.index.event.DirectoryEvent;
import org.aphreet.c3.search.index.event.DirectoryEventListener;

public class CommitIndexThread extends Thread {
	
	private final Log log = LogFactory.getLog(getClass());
	private C3IndexWriter persistentIndexWriter;
	private List<DirectoryEventListener> listeners;
	private Directory currentBuffer;
	private ArrayList<DirectoryEventListener> list;
	
	public CommitIndexThread(ThreadGroup group, Runnable r, String name, C3IndexWriter persistentIndexWriter, List<DirectoryEventListener> listeners) {
        super(group, r, name);
		this.persistentIndexWriter = persistentIndexWriter;
		this.listeners = listeners;
	}

	public C3IndexWriter getPersistentIndexWriter() {
		return persistentIndexWriter;
	}

	public void notifyDirectoryDeleted(Directory directory) {
		this.currentBuffer = directory;
		list = new ArrayList<DirectoryEventListener>(listeners);
		log.info("Notify listeners " + list + " directory deleted.");
		new Thread(new Runnable() {
			@Override
			public void run() {
				DirectoryEvent event = new DirectoryEvent(CommitIndexThread.this, currentBuffer);
				for (DirectoryEventListener listener : list) {
					log.info("Notify listener " + listener + " directory deleted");
					listener.directoryDeleted(event);
				}
			}
		}).start();
	}

	public void notifyDirectoryUpdated(Directory directory) {
		this.currentBuffer = persistentIndexWriter.getDirectory(); // only one reference for multiple threads, may occure wrong event
		list = new ArrayList<DirectoryEventListener>(listeners);
		log.info("Notify listeners " + list + " directory deleted.");
		new Thread(new Runnable() {
			@Override
			public void run() {
				DirectoryEvent event = new DirectoryEvent(CommitIndexThread.this, currentBuffer);
				for (DirectoryEventListener listener : list) {
					log.info("Notify listener " + listener + " directory updated");
					listener.directoryUpdated(event);
				}
			}
		}).start();
	}

}
