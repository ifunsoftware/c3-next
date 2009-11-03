package org.aphreet.c3.web.entity;

public class AbstractSecureEntity implements Entity{

	private static final long serialVersionUID = 6329766032755789063L;
	
	protected int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
