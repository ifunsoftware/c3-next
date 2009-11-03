package org.aphreet.c3.web.webbeans.user;

import java.util.Date;

import javax.annotation.PostConstruct;

import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.entity.UserProfile;
import org.aphreet.c3.web.util.HttpUtil;
import org.aphreet.c3.web.webbeans.RequestBean;
import org.hibernate.validator.Past;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
public class EditUserBean {

	@Autowired
	private RequestBean requestBean;
	
	private User user;

	private String name;
	
	private String family;
	
	@Past
	private Date birthDate;
	
	private String im;

	private String info;
	
	private Boolean notifyMe = false;
	
	@PostConstruct
	public void load(){
		user = requestBean.getCurrentUser();
		
		UserProfile profile = user.getUserProfile();
		
		if(profile != null){
			this.name = profile.getName();
			this.family = profile.getFamily();
			this.im = profile.getIm();
			this.birthDate = profile.getBirthDate();
			this.info = profile.getInfo();
			this.notifyMe = profile.getBoolSetting(UserProfile.NOTIFY_PROP);
		}
	}
	
	public String saveProfile(){
		UserProfile profile;
		if(user.getUserProfile() == null){
			profile = new UserProfile();
			profile.setUser(user);
		}else{
			profile = user.getUserProfile();
		}
		
		profile.setName(this.name);
		profile.setFamily(this.family);
		profile.setIm(this.im);
		profile.setBirthDate(this.birthDate);
		profile.setInfo(this.info);
		profile.addSetting(UserProfile.NOTIFY_PROP, notifyMe);
		user.setUserProfile(profile);
		
		HttpUtil.sendRedirect("/user.xhtml");
		return "success";
	}
	
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFamily() {
		return family;
	}

	public void setFamily(String family) {
		this.family = family;
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public String getIm() {
		return im;
	}

	public void setIm(String im) {
		this.im = im;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public Boolean getNotifyMe() {
		return notifyMe;
	}

	public void setNotifyMe(Boolean notifyMe) {
		this.notifyMe = notifyMe;
	}
}
