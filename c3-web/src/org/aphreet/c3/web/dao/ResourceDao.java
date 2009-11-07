package org.aphreet.c3.web.dao;

import java.util.List;

import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.Document;
import org.aphreet.c3.web.entity.INode;
import org.aphreet.c3.web.entity.Message;
import org.aphreet.c3.web.entity.Content;
import org.aphreet.c3.web.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public class ResourceDao extends SimpleDao{

	public INode getRootNode(User user){
		return (INode) top(getSession()
				.createQuery("from org.aphreet.c3.web.entity.INode node where node.parent is null and node.owner.name = :userName")
				.setString("userName", user.getName()).list());
	}
	
	public INode getRootNode(AbstractGroup group){
		return (INode) top(getSession().getNamedQuery("res_inode_root")
				.setInteger("groupId", group.getId()).list());
	}
	
	public Document getDocumentWithPath(String path){
		return (Document) top(getSession().getNamedQuery("res_document_by_path")
				.setString("path", path).list());
	}
	
	@SuppressWarnings("unchecked")
	public List<Message> getDiscussionList(AbstractGroup group){
		return getSession().createQuery("from org.aphreet.c3.web.entity.Message msg " +
				"where msg.parent is null and msg.group.id = :id order by msg.id desc").setInteger("id", group.getId()).list();
	}
	
	public Content getResourceById(Integer id){
		return (Content) getEntity(id, Content.class);
	}
	
	public Document getDocumentById(Integer id){
		return (Document) getEntity(id, Document.class);
	}
	
	public Message getMessageById(Integer id){
		return (Message) getEntity(id, Message.class);
	}
}
