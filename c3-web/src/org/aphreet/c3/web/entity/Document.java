package org.aphreet.c3.web.entity;

import java.util.List;

import eu.medsea.mimeutil.MimeUtil;

public class Document extends INode{
	
	private static final long serialVersionUID = -4098268403194406161L;

	private String contentAddress;
	
	private String contentType;
	
	private String extension;

	private List<DocumentVersion> versions;
	
	
	public String getHumanReadableSize(){
		return getHeadVersion().getHumanReadableSize();
	}
	
	public void setInodeName(String name){
		String extension = MimeUtil.getExtension(name);
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
		return versions.get(versions.size()-1);
	}
	

	//---------Getters and setters here ---------------
	
	
	public String getContentAddress() {
		return contentAddress;
	}

	public void setContentAddress(String contentAddress) {
		this.contentAddress = contentAddress;
	}

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

}
