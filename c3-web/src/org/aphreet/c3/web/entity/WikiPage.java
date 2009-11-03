package org.aphreet.c3.web.entity;

import java.util.List;

public class WikiPage extends Content{

	private static final long serialVersionUID = 2457161510261377267L;

	private List<WikiPageVersion> versions;

	public List<WikiPageVersion> getVersions() {
		return versions;
	}

	public void setVersions(List<WikiPageVersion> versions) {
		this.versions = versions;
	}
	
	public WikiPageVersion getHeadVersion(){
		return versions.get(versions.size()-1);
	}
	
}
