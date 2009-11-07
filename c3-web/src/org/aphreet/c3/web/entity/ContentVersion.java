package org.aphreet.c3.web.entity;

import java.util.Date;
import java.util.Map;

import org.aphreet.c3.platform.resource.*;

public abstract class ContentVersion implements Entity{

	private static final long serialVersionUID = 512052555366716416L;

	protected User editor;
	
	protected Date editDate;
	
	protected ResourceVersion resourceVersion = new ResourceVersion();

	public User getEditor() {
		return editor;
	}

	public void setEditor(User editor) {
		this.editor = editor;
	}

	public Date getEditDate() {
		return editDate;
	}

	public void setEditDate(Date editDate) {
		this.editDate = editDate;
	}
	
	public Map<String, String> getMetadata(){
		return resourceVersion.getMetadata();
	}
	
	public String getMetadataValue(String key){
		return getMetadata().get(key);
	}
	
	public void setMetadataValue(String key, String value){
		getMetadata().put(key, value);
	}
	
	public DataWrapper getData(){
		return resourceVersion.data();
	}
}
