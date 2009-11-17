package org.aphreet.c3.web.entity;

import java.util.Date;
import java.util.Map;

import org.aphreet.c3.platform.resource.Resource;

public abstract class Content extends AbstractSecureEntity{

	private static final long serialVersionUID = 726315549675680422L;

	protected String title;
	
	protected Date createDate;
	
	protected Date lastEditDate = new Date();
	
	protected User owner;
	
	protected AbstractGroup group;

	private Resource resource = new Resource();
	
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

	public String getResourceAddress() {
		return resourceAddress;
	}

	public void setResourceAddress(String resourceAddress) {
		this.resourceAddress = resourceAddress;
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
	
	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public Date getLastEditDate() {
		return lastEditDate;
	}

	public void setLastEditDate(Date lastEditDate) {
		this.lastEditDate = lastEditDate;
	}

	public void syncMetadata(){
		
		Map<String, String> metadata = getMetadata();
		
		metadata.put(Metadata.TITLE.key(), title);
		metadata.put(Metadata.CREATED.key(), createDate.toString());
		metadata.put(Metadata.OWNER.key(), owner.getName());
		metadata.put(Metadata.POOL.key(), group.getUrlName());
		
	}
	
	public Map<String, String> getMetadata(){
		return getResource().getMetadata();
	}
	
	public Map<String, String> getSysMetadata(){
		return getResource().getSysMetadata();
	}
	
	public String getMetadataValue(String key){
		return getMetadata().get(key);
	}
	
	public void setMetadataValue(String key, String value){
		getMetadata().put(key, value);
	}
	
}
