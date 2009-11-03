package org.aphreet.c3.web.storage;

public class Principal {

	public static Principal STORAGE_CLEANER = new Principal("Storage cleaner");
	
	public Principal(){}
	
	public Principal(String name){
		this.name = name;
	}
	
	protected String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
