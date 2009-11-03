package org.aphreet.c3.web.service.impl;

import java.io.OutputStream;
import java.util.Date;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.web.dao.ResourceDao;
import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.Document;
import org.aphreet.c3.web.entity.DocumentVersion;
import org.aphreet.c3.web.entity.INode;
import org.aphreet.c3.web.entity.Content;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.exception.AccessDeniedException;
import org.aphreet.c3.web.exception.FileSystemException;
import org.aphreet.c3.web.exception.NodeAlreadyExistsException;
import org.aphreet.c3.web.service.IResourceService;
import org.aphreet.c3.web.service.ISearchService;
import org.aphreet.c3.web.service.IStorageService;
import org.aphreet.c3.web.storage.ContentRevision;
import org.aphreet.c3.web.storage.ContentWrapper;
import org.aphreet.c3.web.util.SettingsBean;
import org.aphreet.c3.web.util.collection.CollectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.medsea.mimeutil.MimeUtil;

/**
 * Service provides Resource functionality
 * including VFS
 * @author Mikhail Malygin
 *
 */
@Service
public class ResourceService implements IResourceService{

	private final Log log = LogFactory.getLog(getClass());
	
	@Autowired
	private ResourceDao resourceDao;
	
	@Autowired
	private ISearchService searchService;
	
	@Autowired
	private IStorageService storageStorage;
	
	@Autowired
	private SettingsBean settingsBean;
	
	@PostConstruct
	public void init(){
		
		String detector = settingsBean.get("mimeDetector");
		
		if(detector != null){
			log.info("Using mime detector: " + detector);
			MimeUtil.registerMimeDetector(detector);
		}else{
			log.info("Using default mime detector");
		}
		
		
	}
	
	/**
	 * Add generic node to group
	 * @param node
	 */
	public void addNode(INode node){
		
		if(!node.getGroup().isMember(node.getOwner())){
			throw new AccessDeniedException();
		}
		
		if(node.getId() == 0){
			node.setInodeName(findAvaliableName(node, node.getParent()));
		}
		
		node.setTitle(node.getName());
		node.setCreateDate(new Date());
		
		log.debug("Persisting new node with title: " + node.getTitle());
		
		resourceDao.saveOrUpdate(node);
		
	}	
	private String findAvaliableName(INode node, INode parent){
		
		String name = node.getFullName();
		
		if(parent.getChildWithName(name) == null){
			return node.getFullName();
		}
		
		if(node instanceof Document){
			String ext =  ((Document) node).getExtension();
			String baseName = ((Document) node).getName();
			
			for(int i=0; i< Integer.MAX_VALUE; i++){
				String tryName = baseName + "(" + i + ")." + ext;
				if(parent.getChildWithName(tryName) == null){
					return tryName;
				}
			}
		}else{
			for(int i=0; i< Integer.MAX_VALUE; i++){
				String tryName = name + "(" + i + ")";
				if(parent.getChildWithName(tryName) == null){
					return tryName;
				}
			}
		}
		throw new FileSystemException("Can't generate find name for node");
	}

	/**
	 * Add new Document
	 * 
	 * @param document
	 * @param file - content of new document
	 */
	
	public Document saveDocument(Document document, ContentWrapper content){
		
		User currentUser = document.getOwner();
		
		INode parent = document.getParent();
		
		INode existentNode = parent.getChildWithName(document.getFullName());
		
		if(existentNode != null){
			if(existentNode instanceof Document){
				
				document = (Document) existentNode;
				
				DocumentVersion previousVersion = document.getHeadVersion();
				
				ContentRevision newRevision = storageStorage.updateContent(document.getContentAddress(), content, currentUser);
				
				if(newRevision.getRevision() != previousVersion.getStorageRevision()){
					DocumentVersion newDocVersion = new DocumentVersion();
					
					newDocVersion.setDocument(document);
					newDocVersion.setEditDate(new Date());
					newDocVersion.setEditor(currentUser);
					newDocVersion.setSize(content.length());
					newDocVersion.setStorageRevision(newRevision.getRevision());
					document.getVersions().add(newDocVersion);
				}else{
					if(newRevision.isUpdated()){
						//in case of unversioned storage replace data in current revision
						previousVersion.setSize(content.length());
						previousVersion.setEditor(currentUser);
						previousVersion.setEditDate(new Date());
						
						document.setContentAddress(newRevision.getCa());
					}
				}
				
				
				
			}else{
				throw new NodeAlreadyExistsException("Node with another type already exists");
			}
		}else{
			
			ContentRevision newRevision = storageStorage.addContent(content, currentUser);
		
			document.setContentAddress(newRevision.getCa());
			
			DocumentVersion newVersion = new DocumentVersion();
			
			newVersion.setEditDate(new Date());
			newVersion.setEditor(currentUser);
			newVersion.setSize(content.length());
			newVersion.setDocument(document);
			newVersion.setStorageRevision(newRevision.getRevision());
		
			document.setVersions(CollectionFactory.listOf(newVersion));
		}
		
		document.setContentType(content.getMimeType().toString());
		
		this.addNode(document);
		
		return document;
	}
	
	/**
	 * Remove resource from group
	 * @param resource
	 * @param user
	 */
	public void delete(Content resource, User user){
		if(resource.getGroup().isMember(user)){
			if(resource instanceof INode){
				INode node = (INode)resource;
			
				if(node.getParent() != null){
					node.getParent().getChildren().remove(node);
				}
			}
			resourceDao.delete(resource);
		}
	}

	/**
	 * Returns file system root of group
	 * 
	 * If group root is not exist creates new
	 * 
	 */
	public INode getRootNode(AbstractGroup group) {
		INode node = resourceDao.getRootNode(group);
		if(node == null){
			node = new INode(".", group.getOwner(), false);
			node.setGroup(group);
			resourceDao.persist(node);
		}
		return node;
	}
	
	/**
	 * Find node by id
	 * @param id
	 * @return
	 */
	public INode getNodeWithId(Integer id){
		return (INode) resourceDao.getEntity(id, INode.class);
	}
	
	/**
	 * Find generic resource by id
	 * @param id
	 * @return
	 */
	public Content getResourceById(Integer id){
		return resourceDao.getResourceById(id);
	}
	
	/**
	 * Find Document by real path
	 * @param path
	 * @return
	 */
	public Document getDocumentWithCa(String path){
		return resourceDao.getDocumentWithPath(path);
	}
	
	public void getDocumentContent(Document doc, OutputStream out){
		storageStorage.getContent(doc.getContentAddress(), out);
	}
	
	public void getDocumentContent(Document doc, Integer revision, OutputStream out){
		
		if(revision > doc.getVersions().size() || revision < 1){
			storageStorage.getContent(doc.getContentAddress(), out);
		}else{
			storageStorage.getContent(doc.getContentAddress(), doc.getVersions().get(revision-1).getStorageRevision(), out);
		}
		
		
	}

	@Override
	public INode getINodeWithPath(String path, AbstractGroup group){
		
		INode root = getRootNode(group);
		
		INode result = root;
		
		String[] pathComponents = path.split("/");
		
		for (String string : pathComponents) {
			if(result != null)
				result = result.getChildWithName(string);
			else
				return null;
		}
		
		return result;
	}
	
}
