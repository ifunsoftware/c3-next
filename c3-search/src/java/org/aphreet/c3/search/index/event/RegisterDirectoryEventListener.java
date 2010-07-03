package org.aphreet.c3.search.index.event;

public interface RegisterDirectoryEventListener {
	
	void addDirectoryEventListener(DirectoryEventListener listener);

	void removeDirectoryEventListener(DirectoryEventListener listener);

}
