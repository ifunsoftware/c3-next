package org.aphreet.c3.search.index.event;

public interface DirectoryEventListener {
	
	void directoryDeleted(DirectoryEvent event);

	void directoryAdded(DirectoryEvent event);

	void directoryUpdated(DirectoryEvent event);
}
