package org.aphreet.c3.web.entity;

import java.util.Set;
import org.aphreet.c3.platform.resource.*;

public class Message extends Content{

	private static final long serialVersionUID = 6040160853409422237L;

	private String body;
	
	private Message parent;
	
	private Set<Message> children;

	public void syncMetadata(){
		super.syncMetadata();
		if(this.id <= 0){
			ResourceVersion version = new ResourceVersion();
			version.setData(new DataWrapperFactory().wrap(body));
			getResource().addVersion(version);
		}
		getMetadata().put(Metadata.CONTENT_TYPE.key(), "application/c3-message");
		getSysMetadata().put(Metadata.CONTENT_TYPE.key(), "application/c3-message");
	}
	
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
