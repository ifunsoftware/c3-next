package org.aphreet.c3.search.index;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.aphreet.c3.search.impl.C3MultipleIndexSearcher;
import org.aphreet.c3.search.index.event.DirectoryEvent;
import org.aphreet.c3.search.index.event.DirectoryEventListener;
import org.aphreet.c3.search.index.event.RegisterDirectoryEventListener;

public class C3IndexWriter extends IndexWriter implements RegisterDirectoryEventListener {
	
	private Set<DirectoryEventListener> listeners;
	private Set<Directory> directories;
	private Directory persistentDirectory;

	public C3IndexWriter(File persistentDirectory, Analyzer analyzer, MaxFieldLength mfl)
			throws CorruptIndexException, LockObtainFailedException, IOException {
		super(persistentDirectory, analyzer, mfl);
		listeners = new HashSet<DirectoryEventListener>();
	}
	
	/**
	 * Adds given searcher to listeners list and notifies 
	 * it that a directory has been added.
	 * @param searcher
	 */
	public void registerSearcher(C3MultipleIndexSearcher searcher) {
		listeners.add(searcher);
		searcher.directoryAdded(new DirectoryEvent(this, this.getDirectory()));
	}

	@Override
	public void addDirectoryEventListener(DirectoryEventListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeDirectoryEventListener(DirectoryEventListener listener) {
		listeners.remove(listener);
	}
	
	public synchronized boolean addDirectory(Directory directory) {
		return directories.add(directory);
	}

	
}
