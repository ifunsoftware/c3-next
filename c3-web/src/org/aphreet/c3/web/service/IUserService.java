package org.aphreet.c3.web.service;

import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.User;

public interface IUserService {

	public void createUser(User user);
	
	public void createGroup(AbstractGroup group);
	
	public User getUserByName(String name);
	
	public User getUserWithMail(String mail);
	
	public User getUserById(int id);
	
	public User authUser(String name, String password);
}
