package org.aphreet.c3.web.webbeans;

import java.util.Date;

import javax.annotation.PostConstruct;

import org.aphreet.c3.web.entity.SingleUserGroup;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.service.IPlatformControllService;
import org.aphreet.c3.web.service.IUserService;
import org.aphreet.c3.web.util.HashUtil;
import org.aphreet.c3.web.util.HttpUtil;
import org.aphreet.c3.web.util.collection.CollectionFactory;
import org.hibernate.validator.Email;
import org.hibernate.validator.Length;
import org.hibernate.validator.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
public class InstallBean {

	@Autowired
	private IUserService userService;
	
	@Autowired
	private IPlatformControllService platformControllService;
	
	@Length(min=5, max=64)
	private String password;

	@Email
	private String mail;
	
	@NotEmpty
	private String path;
	
	@PostConstruct
	public void init(){
		
		User adminUser = userService.getUserByName("admin");
		if(adminUser != null){
			HttpUtil.sendNotFound();
		}
		
	}
	
	public String install(){
		
		User user = new User();
		user.setName("admin");
		user.setCreateDate(new Date());
		user.setEnabled(true);
		user.setMail(mail);
		user.setPassword(HashUtil.getSHAHash(password));
		user.setRoles(CollectionFactory.setOf(User.ROLE_SUPERVISOR, User.ROLE_USER));
		
		userService.createUser(user);
		
		SingleUserGroup group = new SingleUserGroup();
		group.setName(user.getName());
		group.setUrlName(user.getName());
		group.setDescription(user.getName() + "'s group");
		group.setOwner(user);
		group.setCreateDate(user.getCreateDate());
		
		user = new User();
		user.setName("anonymous");
		user.setCreateDate(new Date());
		user.setEnabled(false);
		user.setMail("anonymous@localhost");
		user.setPassword("");
		
		userService.createUser(user);

		platformControllService.createStorage("PureBDBStorage", path);
		platformControllService.createStorage("FileBDBStorage", path);
		
		HttpUtil.sendRedirect("/index.xhtml");
		
		return "success";
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

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	
}
