package org.aphreet.c3.web.entity;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aphreet.c3.platform.resource.*;

public class WikiPage extends Content{

	private static final long serialVersionUID = 2457161510261377267L;

	private List<WikiPageVersion> versions = new LinkedList<WikiPageVersion>();

	public List<WikiPageVersion> getVersions() {
		return versions;
	}

	public void setVersions(List<WikiPageVersion> versions) {
		this.versions = versions;
	}
	
	public WikiPageVersion getHeadVersion(){
		return versions.get(versions.size()-1);
	}
	
	public void addVersion(WikiPageVersion wikiVersion){
		ResourceVersion version = new ResourceVersion();
		version.setData(new DataWrapperFactory().wrap(wikiVersion.getBody()));
		Map<String, String> metadata = version.getMetadata();
		metadata.put(Metadata.OWNER.key(), wikiVersion.editor.getName());
		
		resource.addVersion(version);
		
		versions.add(wikiVersion);
		
	}
	
}
