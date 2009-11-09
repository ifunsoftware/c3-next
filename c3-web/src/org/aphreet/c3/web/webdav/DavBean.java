package org.aphreet.c3.web.webdav;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.Document;
import org.aphreet.c3.web.entity.INode;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.service.IGroupService;
import org.aphreet.c3.web.service.IResourceService;
import org.aphreet.c3.web.service.IUserService;
import org.aphreet.c3.platform.resource.*;

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
	
	private static final Log logger = LogFactory.getLog(DavBean.class);
	
	private Set<File> tempFiles = new HashSet<File>();
	
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
		document.getHeadVersion().getData().writeTo(stream);
	}
	
	public void addNode(INode node){
		resourceService.addNode(node);
	}
	
	public Document saveDocument(Document doc, InputStream is) throws IOException{

		File tmpFile = File.createTempFile("c3-dav-upload-" + UUID.randomUUID().toString(), "tmp");
		
		tempFiles.add(tmpFile);

		StreamUtils.readTo(is, tmpFile, true);

		return resourceService.saveDocument(doc, new DataWrapperFactory().wrap(tmpFile));
		
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
	
	@PreDestroy
	public void destroy(){
		for(File tmpFile : tempFiles){
			try{
				tmpFile.delete();
			}catch(Throwable e){
				logger.warn("Failed to remove temp file: " + tmpFile.getAbsolutePath(), e);
			}
		}
	}
}
