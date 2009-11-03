package org.aphreet.c3.web.entity;

public enum Metadata {

	TITLE("title");
	
	private final String key;
	
	private Metadata(String key){
		this.key = key;
	}
	
	public String key(){
		return key;
	}
	
}
