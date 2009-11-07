package org.aphreet.c3.web.entity;

public enum Metadata {

	TITLE("title"),
	POOL("pool"),
	CREATED("created"),
	OWNER("creator"),
	FILE_NAME("filename"),
	FILE_EXT("fileext"),
	CONTENT_TYPE("content.type"),
	URI("uri"),
	COMMENT("comment");
	
	
	private final String key;
	
	private Metadata(String key){
		this.key = key;
	}
	
	public String key(){
		return key;
	}
	
}
