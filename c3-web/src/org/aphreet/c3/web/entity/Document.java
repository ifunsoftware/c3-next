package org.aphreet.c3.web.entity;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.platform.resource.*;
import org.aphreet.c3.web.util.FileUtil;

public class Document extends INode{
	
	private static final long serialVersionUID = -4098268403194406161L;

	private final static Log logger = LogFactory.getLog(Document.class);
	
	private String contentType;
	
	private String extension;

	private List<DocumentVersion> versions = new LinkedList<DocumentVersion>();
	
	
	public void syncMetadata(){
		super.syncMetadata();
		
		Map<String, String> metadata = getMetadata();
		
		metadata.put(Metadata.FILE_EXT.key(), extension);
		metadata.put(Metadata.CONTENT_TYPE.key(), contentType);
		
		getSysMetadata().put(Metadata.CONTENT_TYPE.key(), contentType);
	}
	
	
	public String getHumanReadableSize(){
		return getHeadVersion().getHumanReadableSize();
	}
	
	public void setInodeName(String name){
		String extension = FileUtil.getExtension(name);
		name = name.replaceAll("." + extension + "$", "");
		
		this.name = name;
		this.extension = extension.toLowerCase();
	}
	
	public String getFullName(){
		if(extension != null && !extension.isEmpty()){
			return name + "." + extension;
		}else{
			return name;
		}
	}
	
	@Override
	public Boolean getLeaf() {
		return true;
	}
	
	@Override
	public Boolean isLeaf(){
		return true;
	}
	
	
	public Boolean shouldIndex(){
		return true;
	}
	
	public DocumentVersion getHeadVersion(){
		if(!versions.isEmpty()){
			return versions.get(versions.size()-1);
		}else{
			return null;
		}
		
	}
	

	//---------Getters and setters here ---------------
	
	
	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getExtension(){
		return extension;
	}
	
	public void setExtension(String extension) {
		this.extension = extension;
	}
	
	public List<DocumentVersion> getVersions() {
		return versions;
	}

	public void setVersions(List<DocumentVersion> versions) {
		this.versions = versions;
	}

	public void addNewVersion(DocumentVersion version, DataWrapper data){
		ResourceVersion resVersion = new ResourceVersion();
		resVersion.setData(data);
		
		resource.addVersion(resVersion);
		
		if(resource.isMutable()){
			versions.add(version);
			version.setSize(data.length());
		}else{
			DocumentVersion head = this.getHeadVersion();
			if(head == null){
				version.setSize(data.length());
				versions.add(version);
			}else{
				head.setEditor(version.getEditor());
				head.setEditDate(resVersion.date());
				head.setSize(data.length());
			}
			
			
		}
	}
	
	public void setResource(Resource resource) {
		super.setResource(resource);
		
		List<ResourceVersion> resourceVersions = resource.getVersions();
		
		Iterator<DocumentVersion> docIterator = versions.iterator();
		Iterator<ResourceVersion> resIterator = resourceVersions.iterator();
		
		while(docIterator.hasNext() && resIterator.hasNext()){
			DocumentVersion version = docIterator.next();
			ResourceVersion resVersion = resIterator.next();
			
			version.setResourceVersion(resVersion);
		}
		
		if(docIterator.hasNext() || resIterator.hasNext()){
			logger.warn("Versions inconsitency detected");
		}
		
		
	}
}
 