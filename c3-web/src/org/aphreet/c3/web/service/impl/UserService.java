package org.aphreet.c3.web.service.impl;

import org.aphreet.c3.web.dao.UserDao;
import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService implements IUserService{

	@Autowired
	private UserDao userDao;
	
	@Override
	public User authUser(String name, String password) {
		User user = userDao.getUserByName(name);
		
		boolean authOk = 
			user != null && user.getEnabled() && user.isPasswordCorrect(password);
		
		if(authOk){
			return user;
		}else{
			return null;
		}
	}

	@Override
	public void createGroup(AbstractGroup group) {
		userDao.persist(group);
	}

	@Override
	public void createUser(User user) {
		userDao.persist(user);
	}

	@Override
	public User getUserById(int id) {
		return userDao.getUserById(id);
	}

	@Override
	public User getUserByName(String name) {
		return userDao.getUserByName(name);
	}

	@Override
	public User getUserWithMail(String mail) {
		return userDao.getUserWithMail(mail);
	}

}
