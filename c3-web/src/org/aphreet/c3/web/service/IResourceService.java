package org.aphreet.c3.web.service;

import java.io.OutputStream;

import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.Document;
import org.aphreet.c3.web.entity.INode;
import org.aphreet.c3.web.entity.Content;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.platform.resource.*;

public interface IResourceService {

	/**
	 * Add generic node to group
	 * @param node
	 */
	public void addNode(INode node);

	/**
	 * Saves Document
	 * If parent of document already have document with the same name
	 * updates existent one
	 * 
	 * @param document
	 * @param file - content of new document
	 */
	public Document saveDocument(Document doc, DataWrapper data);

	/**
	 * Remove resource from group
	 * @param resource
	 * @param user
	 */
	public void delete(Content resource, User user);

	/**
	 * Returns file system root of group
	 * 
	 * If group root is not exist creates new
	 * 
	 */
	public INode getRootNode(AbstractGroup group);

	/**
	 * Find node by id
	 * @param id
	 * @return
	 */
	public INode getNodeWithId(Integer id);

	/**
	 * Find generic resource by id
	 * @param id
	 * @return
	 */
	public Content getResourceById(Integer id);

	/**
	 * Find Document by real path
	 * @param path
	 * @return
	 */
	public Document getDocumentWithCa(String path);
	
	public INode getINodeWithPath(String path, AbstractGroup group);

	public void getDocumentContent(Document doc, OutputStream out);
	
	public void getDocumentContent(Document doc, Integer revision, OutputStream out);
	

}