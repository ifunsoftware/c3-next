package org.aphreet.c3.web.webbeans;

import java.util.Date;

import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.service.UserService;
import org.aphreet.springframework.web.HttpParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component
@Scope("request")
public class SimpleBean {

	@Autowired
	private UserService userService;
	
	@HttpParam("id")
	private String id = "";

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String doIt(){
		
		User user = new User();
		user.setName(String.valueOf(new Date().getTime()));
		
		userService.addUser(user);
		
		id="sdasd";
		return "success";
	}
}
