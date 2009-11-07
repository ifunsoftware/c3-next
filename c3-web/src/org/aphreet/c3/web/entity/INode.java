package org.aphreet.c3.web.entity;

import java.util.Date;
import java.util.Set;

public class INode extends Content{

	private static final long serialVersionUID = -6069465501763094482L;
	
	protected String name;
	
	protected INode parent;
	
	protected Set<INode> children;
	
	public INode(){
		
	}
	
	public INode(String name, User user, boolean leaf){
		this.name = name;
		this.owner = user;
		this.createDate = new Date();
		this.title = name;
	}
	
	public void syncMetadata(){
		super.syncMetadata();
		getMetadata().put(Metadata.FILE_NAME.key(), name);
	}
	
	public void setInodeName(String name){
		this.name = name;
	}
	
	public boolean isChildOf(INode parent){
		Set<INode> children = parent.getChildren();
		if(children.contains(this)){
			return true;
		}
		
		for (INode node : children) {
			if(isChildOf(node)){
				return true;
			}
		}
		return false;
	}
	
	public INode getChildWithName(String name){
		for(INode node : this.getChildren()){
			if(node.getFullName().equals(name)){
				return node;
			}
		}
		return null;
	}
	
	public Boolean shouldIndex(){
		return false;
	}
	
	public void setTitle(String title){
		//TODO remove this!
		super.setTitle(title);
		this.name = title;
	}
	
	public String getFullName(){
		return name;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getLeaf() {
		return false;
	}
	
	public Boolean isLeaf(){
		return false;
	}

	public INode getParent() {
		return parent;
	}

	public void setParent(INode parent) {
		this.parent = parent;
	}

	public Set<INode> getChildren() {
		return children;
	}

	public void setChildren(Set<INode> children) {
		this.children = children;
	}
		
	public String toString(){
		return "INode[" + id + " " + name + "]";
	}
	
}
