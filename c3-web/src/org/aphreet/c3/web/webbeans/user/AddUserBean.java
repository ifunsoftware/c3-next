package org.aphreet.c3.web.webbeans.user;

import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.aphreet.c3.web.service.IGroupService;
import org.aphreet.c3.web.service.IUserService;
import org.aphreet.c3.web.entity.SingleUserGroup;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.util.HashUtil;
import org.aphreet.c3.web.util.collection.CollectionFactory;
import org.hibernate.validator.Email;
import org.hibernate.validator.Length;
import org.hibernate.validator.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
public class AddUserBean {

	@Autowired
	private IUserService userService;
	
	@Autowired
	private IGroupService groupService;
	
	@NotEmpty
	@Length(min=3, max=40)
	private String name;
	
	@NotEmpty
	@Length(min=6)
	private String password;
	
	@Email
	private String mail;

	
	public String createAccount(){
		User user = new User();
		user.setName(name);
		user.setMail(mail);
		user.setCreateDate(new Date());
		user.setPassword(HashUtil.getSHAHash(password));
		user.setEnabled(true);
		user.setRoles(CollectionFactory.setOf(User.ROLE_USER));
		
		userService.createUser(user);
		
		SingleUserGroup group = new SingleUserGroup();
		group.setName(user.getName());
		group.setUrlName(user.getName());
		group.setCreateDate(user.getCreateDate());
		group.setOwner(user);
		
		userService.createGroup(group);
		
		return "success";
	}
	
	public void validateName(FacesContext context, 
						UIComponent toValidate, Object value) {
		
		String name = (String) value;
		
		if(userService.getUserByName(name) != null){
			((UIInput) toValidate).setValid(false);
			FacesMessage message = new FacesMessage("This name already exist");
			context.addMessage(toValidate.getClientId(context), message);
		}else if(groupService.getGroupByName(name) != null){
			((UIInput) toValidate).setValid(false);
			FacesMessage message = new FacesMessage("This name already exist");
			context.addMessage(toValidate.getClientId(context), message);
		}
	}

	
	public void validateEmail(FacesContext context, 
			UIComponent toValidate, Object value) {
		
		String email = (String) value;
		
		if (userService.getUserWithMail(email) != null) {
			((UIInput)toValidate).setValid(false);

			FacesMessage message = new FacesMessage("This email already exist");
			context.addMessage(toValidate.getClientId(context), message);
		}
	}
	
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}
}
