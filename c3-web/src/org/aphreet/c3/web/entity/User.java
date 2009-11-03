package org.aphreet.c3.web.entity;

import java.util.Date;
import java.util.Set;

import org.aphreet.c3.web.storage.Principal;
import org.aphreet.c3.web.util.HashUtil;

public class User extends Principal implements Entity{

	private static final long serialVersionUID = 1451053400835269982L;
	
	public final static String ROLE_USER = "ROLE_USER";
	public final static String ROLE_SUPERVISOR = "ROLE_SUPERVISOR";
	
	private int id;
	
	private String password;
	
	private String mail;
	
	private Date createDate;
	
	private Boolean enabled;
	
	private Set<String> roles;
	
	private UserProfile userProfile;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
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

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean active) {
		this.enabled = active;
	}

	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	public UserProfile getUserProfile() {
		return userProfile;
	}

	public void setUserProfile(UserProfile userProfile) {
		this.userProfile = userProfile;
	}
	
	public Boolean isPasswordCorrect(String password){
		return HashUtil.getSHAHash(password).equals(this.password);
	}
}
