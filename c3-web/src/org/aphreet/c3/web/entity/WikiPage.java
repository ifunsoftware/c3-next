package org.aphreet.c3.web.entity;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.platform.resource.*;

public class WikiPage extends Content{

	private static final long serialVersionUID = 2457161510261377267L;
	
	private final static Log logger = LogFactory.getLog(WikiPage.class);
	
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
	
	public void syncMetadata(){
		super.syncMetadata();
		
		getMetadata().put(Metadata.CONTENT_TYPE.key(), "application/c3-wiki");
		getSysMetadata().put(Metadata.CONTENT_TYPE.key(), "application/c3-wiki");
	}
	
	public void addVersion(WikiPageVersion wikiVersion){
		ResourceVersion version = new ResourceVersion();
		version.setData(new DataWrapperFactory().wrap(wikiVersion.getBody()));
		Map<String, String> metadata = version.getMetadata();
		metadata.put(Metadata.OWNER.key(), wikiVersion.editor.getName());
		
		getResource().addVersion(version);
		
		versions.add(wikiVersion);	
	}
	
	public void setResource(Resource resource) {
		super.setResource(resource);
		
		List<ResourceVersion> resourceVersions = getResource().getVersions();
		
		Iterator<WikiPageVersion> wikiIterator = versions.iterator();
		Iterator<ResourceVersion> resIterator = resourceVersions.iterator();
		
		while(wikiIterator.hasNext() && resIterator.hasNext()){
			WikiPageVersion version = wikiIterator.next();
			ResourceVersion resVersion = resIterator.next();
			
			version.setResourceVersion(resVersion);
		}
		
		if(wikiIterator.hasNext() || resIterator.hasNext()){
			logger.warn("Versions inconsitency detected");
		}	
	}
}
