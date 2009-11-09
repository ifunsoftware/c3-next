package org.aphreet.c3.web.dao.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.platform.access.*;
import org.aphreet.c3.platform.resource.*;
import org.aphreet.c3.web.entity.Content;
import org.aphreet.c3.web.entity.Document;
import org.aphreet.c3.web.entity.Message;
import org.aphreet.c3.web.entity.WikiPage;
import org.hibernate.event.PostLoadEvent;
import org.hibernate.event.PostLoadEventListener;
import org.hibernate.event.PreInsertEvent;
import org.hibernate.event.PreInsertEventListener;
import org.hibernate.event.PreUpdateEvent;
import org.hibernate.event.PreUpdateEventListener;

public class ResourcePersistenceListener implements PostLoadEventListener, PreUpdateEventListener, PreInsertEventListener{

	private static final long serialVersionUID = 3669997536268933447L;
	
	private PlatformAccessEndpoint platformAccessEndpoint;
	
	public void setPlatformAccessEndpoint(
			PlatformAccessEndpoint platformAccessEndpoint) {
		this.platformAccessEndpoint = platformAccessEndpoint;
	}

	private final Log logger = LogFactory.getLog(getClass());
	
	@Override
	public void onPostLoad(PostLoadEvent event) {
		Object entity = event.getEntity();
		
		if(isContent(entity)){
			Content content = (Content) entity;
			
			content.getResourceAddress();
			
			Resource resource = platformAccessEndpoint.get(content.getResourceAddress());
			content.setResource(resource);
		}
	}

	@Override
	public boolean onPreUpdate(PreUpdateEvent event) {
		Object entity = event.getEntity();
		
		if(isContent(entity)){
			Content content = (Content) entity;
			content.syncMetadata();
			
			String address = platformAccessEndpoint.update(content.getResource());
			content.setResourceAddress(address);
			
		}
		
		return false;
	}

	@Override
	public boolean onPreInsert(PreInsertEvent event) {
		Object entity = event.getEntity();
		
		if(isContent(entity)){
			logger.info("persisting new resource");
			Content content = (Content) entity;
			content.syncMetadata();
			
			String address = platformAccessEndpoint.add(content.getResource());
			content.setResourceAddress(address);
			logger.info("new content address: " + address);
		}
		
		return false;
	}
	
	
	private boolean isContent(Object entity){
		return entity instanceof Message || entity instanceof WikiPage || entity instanceof Document;
	}

}
