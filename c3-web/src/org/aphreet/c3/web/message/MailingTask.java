package org.aphreet.c3.web.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class MailingTask implements Serializable{

	private static final long serialVersionUID = 3518468607072823895L;

	private String title;
	
	private String message;
	
	private String group;
	
	private ArrayList<String> addresses;

	public MailingTask(String title, String message, String group, Collection<String> addresses){
		this.addresses = new ArrayList<String>(addresses);
		this.title = title;
		this.message = message;
		this.group = group;
	}
	
	public String getGroup() {
		return group;
	}
	
	public String getTitle() {
		return title;
	}

	public String getMessage() {
		return message;
	}

	public ArrayList<String> getAddresses() {
		return addresses;
	}
	
	public String toString(){
		return "title:'" + title + "', body:'" + message +"' to:" + addresses.toString(); 
	}
}
