package org.aphreet.c3.web.webbeans.group;

import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.aphreet.c3.web.entity.WorkGroup;
import org.aphreet.c3.web.service.IGroupService;
import org.aphreet.c3.web.util.HttpUtil;
import org.aphreet.c3.web.webbeans.RequestBean;
import org.hibernate.validator.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
public class AddGroupBean {

	@Autowired private IGroupService groupService;
	
	@Autowired private RequestBean requestBean;
	
	@NotEmpty
	private String name;
	
	@NotEmpty
	private String urlName;
	
	@NotEmpty
	private String description;
	
	public String createGroup(){
		WorkGroup group = new WorkGroup();
		group.setName(name);
		group.setUrlName(urlName);
		group.setDescription(description);
		group.setCreateDate(new Date());
		group.setOwner(requestBean.getCurrentUser());
		groupService.createGroup(group);

		HttpUtil.sendRedirect("/group/main.xhtml?id=" + group.getId());
		return "success";
	}
	
	public void validateName(FacesContext context, 
			UIComponent toValidate, Object value) {

		String name = (String) value;

		
		if(groupService.isGroupNameExist(name)){
			((UIInput) toValidate).setValid(false);
			FacesMessage message = new FacesMessage("This name already exist");
			context.addMessage(toValidate.getClientId(context), message);
		}
	}
	
	public void validateUrlName(FacesContext context, 
			UIComponent toValidate, Object value) {
		
		String name = (String) value;

		
		if(groupService.isGroupUrlNameExist(name)){
			((UIInput) toValidate).setValid(false);
			FacesMessage message = new FacesMessage("This name already exist");
			context.addMessage(toValidate.getClientId(context), message);
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUrlName() {
		return urlName;
	}

	public void setUrlName(String urlName) {
		this.urlName = urlName;
	}
	
}
