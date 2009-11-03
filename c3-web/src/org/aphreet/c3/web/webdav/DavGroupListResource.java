package org.aphreet.c3.web.webdav;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aphreet.c3.web.entity.AbstractGroup;

import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.Resource;

public class DavGroupListResource extends AbstractDavResource implements CollectionResource{

	private boolean loaded = false;
	
	private List<Resource> children = new ArrayList<Resource>();
	
	private Map<String, Resource> childrenMap = new HashMap<String, Resource>();
	
	@Override
	public Resource child(String arg0) {
		load();
		return childrenMap.get(arg0);
	}

	@Override
	public List<? extends Resource> getChildren() {
		load();
		return children;
	}
	
	@Override
	public String getName() {
		return "dav";
	}
	
	private void load(){
		if(!loaded){
			
			Set<AbstractGroup> groups = davBean.getGroupsForUser(currentUser);
	
			for (AbstractGroup abstractGroup : groups) {
				
				Resource davResource = new DavGroupResource(abstractGroup);
				
				children.add(davResource);
				childrenMap.put(abstractGroup.getUrlName(), davResource);
			}
			
			loaded = true;
		}
	}

	@Override
	public Date getCreateDate() {
		return currentUser.getCreateDate();
	}

}
