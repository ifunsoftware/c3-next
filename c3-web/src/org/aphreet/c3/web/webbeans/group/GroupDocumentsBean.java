package org.aphreet.c3.web.webbeans.group;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;

import org.ajax4jsf.context.AjaxContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.platform.resource.*;
import org.aphreet.c3.web.entity.Document;
import org.aphreet.c3.web.entity.INode;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.exception.FileSystemException;
import org.aphreet.c3.web.service.IResourceService;
import org.aphreet.c3.web.util.FacesUtil;
import org.aphreet.c3.web.util.FileUtil;
import org.aphreet.c3.web.util.HttpUtil;
import org.hibernate.validator.NotEmpty;
import org.richfaces.component.html.HtmlTree;
import org.richfaces.event.DropEvent;
import org.richfaces.event.DropListener;
import org.richfaces.event.FileUploadListener;
import org.richfaces.event.NodeSelectedEvent;
import org.richfaces.event.NodeSelectedListener;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.ListRowKey;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;
import org.richfaces.model.UploadItem;
import org.springframework.beans.factory.annotation.Autowired;

public class GroupDocumentsBean extends IdGroupViewBean 
	implements FileUploadListener, DropListener, NodeSelectedListener, ValueChangeListener{

	@Autowired private IResourceService resourceService;
	
	private final static String SELECTED_ROW_KEY = "selected-row";

	private final static Log logger = LogFactory.getLog(GroupDocumentsBean.class);
	
	private List<File> uploadData;
	
	private TreeNode<INode> fileTree;
	
	@NotEmpty
	private String folderName;
	
	@NotEmpty
	private String nodeName;
	
	private Set<File> tempFiles = new HashSet<File>();
	
	private INode currentNode;

	private int moveId = -1;
	
	private int oldParentId = -1;
	
	@Override
	protected void load() {
		
		if(!isMember){
			HttpUtil.sendAccessDenied();
			return;
		}
		
		fileTree = new TreeNodeImpl<INode>();
		fileTree.setData(new INode());
		
		INode root = resourceService.getRootNode(group);
		TreeNode<INode> treeNode = new TreeNodeImpl<INode>();
		treeNode.setData(root);
		fileTree.addChild(Integer.valueOf(root.getId()), treeNode);
		
		pushChildren(root, treeNode);
	}
	
	public void processUpload(UploadEvent event) {
		logger.info("processing upload: " + event.toString());
		List<UploadItem> uploadItems = event.getUploadItems();
		
		INode parent = getCurrentParent();
		
		User currentUser = requestBean.getCurrentUser();
		
		for (UploadItem uploadItem : uploadItems) {
			Document doc = new Document();
			
			String name = findAvaliableName(uploadItem.getFileName(), parent);
			
			doc.setInodeName(name);
			doc.setParent(parent);
			doc.setGroup(group);
			doc.setOwner(currentUser);
			
			if(uploadItem.isTempFile()){
				File tmpFile = uploadItem.getFile();
				tempFiles.add(tmpFile);
				resourceService.saveDocument(doc, new DataWrapperFactory().wrap(tmpFile));
			}else{
				resourceService.saveDocument(doc, new DataWrapperFactory().wrap(uploadItem.getData()));
			}
		}
	}
	public void deleteDoc(){
		//int idDoc = currentNode.getId();
		HtmlTree tree = (HtmlTree) FacesUtil.findComponent("file_tree");
		Integer id = (Integer) tree.getAttributes().get(SELECTED_ROW_KEY);
		INode node = resourceService.getNodeWithId(id);
		resourceService.delete(node, requestBean.getCurrentUser());
	}
	public String addFolder(){
		INode inode = new INode();
		inode.setInodeName(folderName);
		inode.setParent(getCurrentParent());
		inode.setGroup(group);

		inode.setOwner(requestBean.getCurrentUser());
		

		resourceService.addNode(inode);
		folderName = "";
		return "success";
	}
	
	public String editNode(){
		HtmlTree tree = (HtmlTree) FacesUtil.findComponent("file_tree");
		
		Integer id = (Integer) tree.getAttributes().get(SELECTED_ROW_KEY);
		
		INode node = resourceService.getNodeWithId(id);
		if(node != null){
			node.setInodeName(nodeName);
		}	
		return "success";
	}
	
	
	
	public void processDrop(DropEvent event) {
		
		HtmlTree tree = (HtmlTree) FacesUtil.findComponent("file_tree");
		
		event.getSource();
		
		ListRowKey<?> dragKey = (ListRowKey<?>) event.getDragValue();
		ListRowKey<?> dropKey = (ListRowKey<?>) event.getDropValue();
		
		INode movingNode = (INode) tree.getRowData(dragKey);
		INode targetNode = (INode) tree.getRowData(dropKey);
		
		if(movingNode.getId() != targetNode.getId()){
			if(!targetNode.isChildOf(movingNode)){
				//moving node to another parent
				//movingNode.getParent().getChildren().remove(movingNode);
				
				moveId = movingNode.getId();
				oldParentId = movingNode.getParent().getId();
				
				
				movingNode.setParent(targetNode);
				targetNode.getChildren().add(movingNode);
				
				
				
				load();
				
				AjaxContext ac = AjaxContext.getCurrentInstance();
				ac.addComponentToAjaxRender(tree);
			}
		}
	}
	
	
	public void processValueChange(ValueChangeEvent event)
								throws AbortProcessingException {
		
		Integer id = getSelectedNodeId();
		if(id != null){
			INode node = resourceService.getNodeWithId(id);
			currentNode = node;
			nodeName = currentNode.getName();
		}
	}
	
	public void processSelection(NodeSelectedEvent event)
		throws AbortProcessingException {
		
		try{
			HtmlTree tree = (HtmlTree) event.getComponent();
			
			//FIXME exceptions somewhere here
			INode selectedNode = (INode) tree.getRowData();
			
			currentNode = selectedNode;
			nodeName = currentNode.getName();

			//saving last selected row
			tree.getAttributes().put(SELECTED_ROW_KEY, Integer.valueOf(currentNode.getId()));
		
		}catch(RuntimeException e){
			//there is warn with IllegalStateException somewhere here
			//and I don't know where =(
			//e.printStackTrace();
			throw e;
		}
	}
	
	//------------------- Private methods------------------------
	
	private void pushChildren(INode file, TreeNode<INode> node){
		Set<INode> children = file.getChildren();
		if(children != null){
			for (INode node2 : children) {
				
				if(file.getId() == oldParentId && node2.getId() == moveId) continue;
				
				TreeNode<INode> treeNode = new TreeNodeImpl<INode>();
				treeNode.setData(node2);
				node.addChild(Integer.valueOf(node2.getId()), treeNode);
				pushChildren(node2, treeNode);
			}
		}
	}
	
	private INode getCurrentParent(){
		INode parent = null;
		
		Integer id = getSelectedNodeId();
		if(id != null){
			parent = resourceService.getNodeWithId(id);
			if(parent.isLeaf()){
				parent = parent.getParent();
			}
		}
		
		if(parent == null){
			parent = resourceService.getRootNode(group);
		}

		return parent;
	}
	
	private Integer getSelectedNodeId(){
		HtmlTree tree = (HtmlTree) FacesUtil.findComponent("file_tree");
		Integer id = (Integer) tree.getAttributes().get(SELECTED_ROW_KEY);
		return id;
	}
	
	private String findAvaliableName(String name, INode parent){
		if(parent.getChildWithName(name) == null){
			return name;
		}
		
		String ext = FileUtil.getExtension(name);
		String baseName = name.replaceAll("[.]" + ext + "$", "");
		
		for(int i=0; i< Integer.MAX_VALUE; i++){
			String tryName = baseName + "(" + i + ")." + ext;
			if(parent.getChildWithName(tryName) == null){
				return tryName;
			}
		}
		throw new FileSystemException("Can't generate find name for node");
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
	
	//-------------- getters and setters -------------------
	public TreeNode<INode> getFileTree() {
		return fileTree;
	}

	public void setFileTree(TreeNode<INode> fileTree) {
		this.fileTree = fileTree;
	}


	public List<File> getUploadData() {
		return uploadData;
	}


	public void setUploadData(List<File> uploadData) {
		logger.info("upload data setter invoked");
		this.uploadData = uploadData;
	}


	public String getFolderName() {
		return folderName;
	}


	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}

	public INode getCurrentNode() {
		return currentNode;
	}


	public void setCurrentNode(INode currentNode) {
		this.currentNode = currentNode;
	}


	public String getNodeName() {
		return nodeName;
	}


	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
}
