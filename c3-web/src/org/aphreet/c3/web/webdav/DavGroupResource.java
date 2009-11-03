package org.aphreet.c3.web.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.Document;
import org.aphreet.c3.web.entity.INode;
import org.aphreet.c3.web.webdav.exception.DavException;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.MakeCollectionableResource;
import com.bradmcevoy.http.PutableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

public class DavGroupResource extends AbstractDavResource implements CollectionResource, MakeCollectionableResource, PutableResource{

	private AbstractGroup group;

	private List<Resource> children = new ArrayList<Resource>();
	
	private Map<String, Resource> childrenMap = new HashMap<String, Resource>();
	
	private boolean loaded = false;
	
	public DavGroupResource(String groupName){
		AbstractGroup group = davBean.getGroupByName(groupName);
		if(group == null){
			throw new DavException("can't find group with name: " + groupName);
		}
		this.group = group;
	}
	
	public DavGroupResource(AbstractGroup group){
		this.group = group;
	}
	
	@Override
	public boolean authorise(Request request, Method method, Auth auth) {
		
		boolean authed = super.authorise(request, method, auth);
		
		return authed && group.isMember(currentUser);
	}
	
	private void load(){
		if(!loaded){
			if(!group.isMember(currentUser)){
				throw new DavException("user is not member of group");
			}
			
			Set<INode> nodes = davBean.getRootNodes(group, currentUser);
			
			for (INode iNode : nodes) {
				
				Resource resource = DavNodeResource.createResource(group, iNode);
				children.add(resource);
				childrenMap.put(iNode.getFullName(), resource);
			}
			
			loaded = true;
		}
	}
	
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
		return group.getUrlName();
	}

	@Override
	public Date getCreateDate() {
		return group.getCreateDate();
	}
	
	@Override
	public CollectionResource createCollection(String arg0)
			throws NotAuthorizedException, ConflictException {
		
		INode parent = davBean.getRootNode(group, currentUser);
		
		INode node = new INode();
		node.setGroup(group);
		node.setOwner(currentUser);
		node.setCreateDate(new Date());
		node.setTitle(arg0);
		node.setInodeName(arg0);
		node.setChildren(new HashSet<INode>());
		
		node.setParent(parent);
		
		davBean.addNode(node);
		
		CollectionResource newResource = new DavFolder(node, group);
		
		childrenMap.put(node.getFullName(), newResource);
		children.add(newResource);
		
		return newResource;
	}

	@Override
	public Resource createNew(String name, InputStream is, Long length,
			String contentType) throws IOException {
		
		INode node = davBean.getRootNode(group, currentUser);
		
		Document document = new Document();
		document.setInodeName(name);
		document.setOwner(currentUser);
		document.setGroup(group);
		document.setParent(node);
		
		document = davBean.saveDocument(document, is);
		
		return DavNodeResource.createResource(group, document);
	}

}
