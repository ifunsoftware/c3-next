package org.aphreet.c3.web.entity;

public class Reference extends Content{

	private static final long serialVersionUID = 7136810948260249662L;

	private String uri;
	
	private String comment;
	
	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
}
