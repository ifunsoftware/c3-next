package org.aphreet.c3.web.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.Document;
import org.aphreet.c3.web.entity.INode;
import org.aphreet.c3.web.util.SpringUtil;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.DeletableResource;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.MakeCollectionableResource;
import com.bradmcevoy.http.MoveableResource;
import com.bradmcevoy.http.PutableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

public abstract class DavNodeResource extends AbstractDavResource implements DeletableResource, MoveableResource{

	protected final INode node;
	protected final AbstractGroup group;
	
	public DavNodeResource(INode node, AbstractGroup group){
		this.node = node;
		this.group = group;
	}
	
	@Override
	public String getName() {
		return node.getFullName();
	}

	@Override
	public Date getCreateDate() {
		return node.getCreateDate();
	}
	
	@Override
	public void delete() {
		davBean.deleteNode(node, currentUser);
	}
	
	@Override
	public void moveTo(CollectionResource res, String name)
			throws ConflictException {
		
		if(res instanceof DavFolder){
			INode newParent = ((DavFolder) res).getNode();
			AbstractGroup group = ((DavFolder) res).getGroup();
			
			if(group == this.group){
				if(newParent.isChildOf(this.node)){
					throw new ConflictException(this);
				}
				
				if(newParent.getChildWithName(name) != null){
					throw new ConflictException(this);
				}
				
				this.node.setParent(newParent);
				this.node.setInodeName(name);
				return;
				
			}else{
				throw new ConflictException(this);
			}
			
			
		}else if(res instanceof DavGroupResource){
			INode newRoot = davBean.getRootNode(group, currentUser);
			if(newRoot != null){
				this.node.setParent(newRoot);
				this.node.setInodeName(name);
			}
		}else{
			throw new ConflictException(this);
		}
		
	}
	
	@Override
	public boolean authorise(Request request, Method method, Auth auth) {
		boolean authed = super.authorise(request, method, auth);
		return authed && group.isMember(currentUser);
	}
	
	public INode getNode() {
		return node;
	}


	public AbstractGroup getGroup() {
		return group;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DavNodeResource other = (DavNodeResource) obj;
		if (node == null) {
			if (other.node != null)
				return false;
		} else if (node != other.node)
			return false;
		return true;
	}
	
	
	public static DavNodeResource createResource(String groupname, String path){
		DavBean davBean = (DavBean) SpringUtil.getBean("davBean");
		
		AbstractGroup group = davBean.getGroupByName(groupname);
		
		if(group != null){
			INode node = davBean.getNodeByPath(path, group);
			return createResource(group, node);
		}
		
		return null;
	}
	
	public static DavNodeResource createResource(AbstractGroup group, INode node){
		if(node != null){
			if(node.isLeaf()){
				return new DavDocument(node, group);
			}else{
				return new DavFolder(node, group);
			}
		}

		return null;
	}


	
	
}

class DavDocument extends DavNodeResource implements GetableResource{

	private Document document;
	
	public DavDocument(INode node, AbstractGroup group) {
		super(node, group);
		document = (Document) node;
	}

	@Override
	public Date getModifiedDate() {
		return document.getHeadVersion().getEditDate();
	}

	@Override
	public Long getContentLength() {
		return document.getHeadVersion().getSize();
	}

	@Override
	public String getContentType(String arg0) {
		return document.getContentType();
	}

	@Override
	public Long getMaxAgeSeconds(Auth arg0) {
		return 10l;
	}

	@Override
	public void sendContent(OutputStream arg0, Range arg1,
			Map<String, String> arg2, String arg3) throws IOException,
			NotAuthorizedException {

		davBean.writeDocumentToOs(document, arg0);
		
	}
	
}

class DavFolder extends DavNodeResource implements PutableResource, MakeCollectionableResource, CollectionResource{

	private ArrayList<Resource> children = new ArrayList<Resource>();
	
	private Map<String, Resource> childrenMap = new HashMap<String, Resource>();
	
	public DavFolder(INode node, AbstractGroup group) {
		super(node, group);
		
		for (INode child : node.getChildren()) {
			Resource resource = DavNodeResource.createResource(group, child);
			children.add(resource);
			childrenMap.put(child.getFullName(), resource);
		}
	}

	@Override
	public Resource createNew(String name, InputStream is, Long length,
			String contentType) throws IOException {
		
		Document document = new Document();
		document.setInodeName(name);
		document.setOwner(currentUser);
		document.setGroup(group);
		document.setParent(node);
		
		document = davBean.saveDocument(document, is);
		
		return DavNodeResource.createResource(group, document);
	}

	@Override
	public Resource child(String arg0) {
		
		return childrenMap.get(arg0);
	}

	@Override
	public List<? extends Resource> getChildren() {
		
		return children;
	}

	

	@Override
	public CollectionResource createCollection(String arg0)
			throws NotAuthorizedException, ConflictException {
		
		INode node = new INode();
		node.setGroup(group);
		node.setOwner(currentUser);
		node.setCreateDate(new Date());
		node.setTitle(arg0);
		node.setInodeName(arg0);
		node.setChildren(new HashSet<INode>());
		
		node.setParent(this.node);
		
		davBean.addNode(node);
		
		CollectionResource newResource = new DavFolder(node, group);
		
		childrenMap.put(node.getFullName(), newResource);
		children.add(newResource);
		
		return newResource;
	}
	
	@Override
	public Date getModifiedDate() {
		return new Date();
	}
}
