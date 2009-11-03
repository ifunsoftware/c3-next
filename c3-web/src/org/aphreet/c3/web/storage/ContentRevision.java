package org.aphreet.c3.web.storage;

public class ContentRevision {

	private final long revision;
	
	private final String ca;
	
	private final boolean updated;

	public ContentRevision(String ca, long revision, boolean updated) {
		super();
		this.revision = revision;
		this.ca = ca;
		this.updated = updated;
	}

	public long getRevision() {
		return revision;
	}

	public String getCa() {
		return ca;
	}
	
	public boolean isUpdated() {
		return updated;
	}

	
}
