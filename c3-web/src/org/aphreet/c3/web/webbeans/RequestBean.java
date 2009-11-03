package org.aphreet.c3.web.webbeans;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.service.IUserService;
import org.aphreet.c3.web.util.HttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
public class RequestBean {

	@Autowired
	private IUserService userService;
	
	private User currentUser;
	
	@SuppressWarnings("unused")
	private static final Log logger = LogFactory.getLog(RequestBean.class);
	
	@PostConstruct
	public void load(){
		String userName = null;
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		
		
		
		if(auth != null){
			userName = auth.getName();
		}else{
			userName = "anonymous";
		}
		
		currentUser = userService.getUserByName(userName);
	}
	
	public User getCurrentUser(){
		return currentUser;
	}
	
	public Boolean getAnonymous(){
		return currentUser.getName().equals("anonymous");
	}
	
	public String getId(){
		String id = HttpUtil.getParameter("id");
		if(id != null){
			return id;
		}else{
			return "";
		}
	}
	
}
