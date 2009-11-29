package org.aphreet.c3.web.entity;

public class WikiPageVersion extends ContentVersion{

	private static final long serialVersionUID = 8049013453614108340L;

	private int id;
	
	private String body;
	
	private String htmlBody;
	
	private String comment = "";
	
	private WikiPage page;

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getHtmlBody() {
		return htmlBody;
	}

	public void setHtmlBody(String htmlBody) {
		this.htmlBody = htmlBody;
	}
	
	public WikiPage getPage() {
		return page;
	}

	public void setPage(WikiPage page) {
		this.page = page;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
	
}
