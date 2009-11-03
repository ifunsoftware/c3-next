package org.aphreet.c3.web.entity;

import java.util.Date;
import java.util.Map;

import org.aphreet.c3.platform.resource.Resource;

public abstract class Content extends AbstractSecureEntity{

	private static final long serialVersionUID = 726315549675680422L;

	protected String title;
	
	protected Date createDate;
	
	protected User owner;
	
	protected AbstractGroup group;

	protected Resource resource;
	
	protected String resourceAddress;
	
	public Boolean shouldIndex(){
		return true;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
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

	public AbstractGroup getGroup() {
		return group;
	}

	public void setGroup(AbstractGroup group) {
		this.group = group;
	}
	
	public void syncMetadata(){
		getMetadata().put(Metadata.TITLE.key(), title);
	}
	
	public Map<String, String> getMetadata(){
		return resource.getMetadata();
	}
	
	public String getMetadataValue(String key){
		return getMetadata().get(key);
	}
	
	public void setMetadataValue(String key, String value){
		getMetadata().put(key, value);
	}
	
}
