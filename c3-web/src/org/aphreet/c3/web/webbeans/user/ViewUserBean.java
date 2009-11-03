package org.aphreet.c3.web.webbeans.user;

import javax.annotation.PostConstruct;

import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.entity.UserProfile;
import org.aphreet.c3.web.service.IUserService;
import org.aphreet.c3.web.util.HttpUtil;
import org.aphreet.c3.web.webbeans.RequestBean;
import org.aphreet.springframework.web.HttpParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
public class ViewUserBean {

	@Autowired
	private IUserService userService;
	
	@Autowired
	private RequestBean requestBean;
	
	private User user;
	
	private UserProfile userProfile;
	
	@HttpParam("id")
	private Integer userId;
	
	private boolean allowEdit;

	private String userName;
	
	@PostConstruct
	public void load(){
		User currentUser = requestBean.getCurrentUser();
		
		if(userId == null){
			user = currentUser;		
		}else{
			user = userService.getUserById(userId);
		}
		
		if(user != null){
			userProfile = user.getUserProfile();
			if(userProfile == null){
				userProfile = new UserProfile();
			}
			userName = user.getName();
			allowEdit = user == currentUser;
		}else{
			HttpUtil.sendNotFound();
		}	
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public UserProfile getUserProfile() {
		return userProfile;
	}

	public void setUserProfile(UserProfile userProfile) {
		this.userProfile = userProfile;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public boolean isAllowEdit() {
		return allowEdit;
	}

	public void setAllowEdit(boolean allowEdit) {
		this.allowEdit = allowEdit;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
}
