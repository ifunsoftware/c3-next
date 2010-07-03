package org.aphreet.c3.search.index.event;

import java.util.EventObject;

import org.apache.lucene.store.Directory;

public class DirectoryEvent extends EventObject {

	private static final long serialVersionUID = 1L;
	private Directory directory;
	
	/**
	 * @param source of the event
	 * @param directory implementation of {@linkplain org.apache.lucene.store.Directory} 
	 * 		  whose state changed 
	 */
	public DirectoryEvent(Object source, Directory directory) {
		super(source);
		this.directory = directory;
	}

	public Directory getDirectory() {
		return directory;
	}

}
