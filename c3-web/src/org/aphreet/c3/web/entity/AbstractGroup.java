package org.aphreet.c3.web.entity;

import java.util.Date;
import java.util.Set;

public abstract class AbstractGroup implements Entity{

	private static final long serialVersionUID = 4061177828203610792L;
	
	protected int id;

	protected String name;
	
	protected String urlName;
	
	protected String description;

	protected Date createDate;

	protected User owner;
	
	public abstract boolean isMember(User user);
	
	public abstract Set<User> getAllMembers();
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	
	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}
	
	public String getUrlName() {
		return urlName;
	}

	public void setUrlName(String urlName) {
		this.urlName = urlName;
	}

}
