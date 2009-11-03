package org.aphreet.c3.web.webbeans.document;

import org.aphreet.c3.web.entity.Document;
import org.aphreet.c3.web.entity.INode;
import org.aphreet.c3.web.entity.WorkGroup;
import org.aphreet.c3.web.exception.NotFoundException;
import org.aphreet.c3.web.service.IResourceService;
import org.aphreet.c3.web.webbeans.group.AbstractGroupViewBean;
import org.aphreet.springframework.web.HttpParam;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractDocumentBean extends AbstractGroupViewBean{

	@HttpParam
	private Integer id;
	
	@Autowired
	protected IResourceService resourceService;
	
	protected Document document;
	
	@Override
	protected WorkGroup loadGroup() throws NotFoundException {
		INode node = resourceService.getNodeWithId(id);
		
		if(!(node instanceof Document)){
			throw new NotFoundException();
		}
		
		document = (Document) node;
		
		return (WorkGroup) node.getGroup();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

}
