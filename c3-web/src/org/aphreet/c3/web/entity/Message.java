package org.aphreet.c3.web.entity;

import java.util.Set;

public class Message extends Content{

	private static final long serialVersionUID = 6040160853409422237L;

	private String body;
	
	private Message parent;
	
	private Set<Message> children;

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public Message getParent() {
		return parent;
	}

	public void setParent(Message parent) {
		this.parent = parent;
	}

	public Set<Message> getChildren() {
		return children;
	}

	public void setChildren(Set<Message> children) {
		this.children = children;
	}
}
