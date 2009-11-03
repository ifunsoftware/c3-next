package org.aphreet.c3.web.webdav;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.Document;
import org.aphreet.c3.web.entity.INode;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.service.IGroupService;
import org.aphreet.c3.web.service.IResourceService;
import org.aphreet.c3.web.service.IUserService;
import org.aphreet.c3.web.storage.ContentWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.bradmcevoy.io.StreamUtils;

@Component
@Scope("request")
public class DavBean {

	@Autowired
	private IResourceService resourceService;
	
	@Autowired
	private IUserService userService;

	@Autowired
	private IGroupService groupService;
	
	public Set<AbstractGroup> getGroupsForUser(User user){
		
		if(user != null){
			return groupService.getGroupsForUser(user);
		}else{
			return null;
		}
	}
	
	public AbstractGroup getGroupByName(String group){
		return groupService.getGroupByName(group);
	}
	
	public INode getNodeByPath(String path, AbstractGroup group){
		return resourceService.getINodeWithPath(path, group);
	}
	
	public User getUserByName(String name){
		return userService.getUserByName(name);
	}
	
	public void deleteNode(INode node, User currentUser){
		resourceService.delete(node, currentUser);
	}
	
	public INode getNodeForPath(String path, AbstractGroup group){
		return resourceService.getINodeWithPath(path, group);
	}
	
	public void writeDocumentToOs(Document document, OutputStream stream){
		resourceService.getDocumentContent(document, stream);
	}
	
	public void addNode(INode node){
		resourceService.addNode(node);
	}
	
	public Document saveDocument(Document doc, InputStream is) throws IOException{

		File tmpFile = File.createTempFile("c3-dav-upload-" + UUID.randomUUID().toString(), "tmp");
		
		try{
			StreamUtils.readTo(is, tmpFile, true);

			return resourceService.saveDocument(doc, ContentWrapper.wrap(tmpFile));
		}finally{
			tmpFile.delete();
		}
	}
	
	public Set<INode> getRootNodes(AbstractGroup group, User user){
		INode root = resourceService.getRootNode(group);
		if(root != null){
			return root.getChildren();
		}
		return null;
	}
	
	public INode getRootNode(AbstractGroup group, User user){
		return resourceService.getRootNode(group);
	}
	
	public User authenticate(String userName, String password){
		return userService.authUser(userName, password);
	}
}
