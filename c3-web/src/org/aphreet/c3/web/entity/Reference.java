package org.aphreet.c3.web.entity;

import org.aphreet.c3.platform.resource.DataWrapperFactory;
import org.aphreet.c3.platform.resource.ResourceVersion;

public class Reference extends Content{

	private static final long serialVersionUID = 7136810948260249662L;

	private String uri;
	
	private String comment;
	
	
	public void syncMetadata(){
		super.syncMetadata();
		getMetadata().put(Metadata.COMMENT.key(), comment);
		getMetadata().put(Metadata.URI.key(), uri);
		if(this.id <= 0){
			ResourceVersion version = new ResourceVersion();
			version.setData(new DataWrapperFactory().empty());
			getResource().addVersion(version);
		}
	}
	
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
