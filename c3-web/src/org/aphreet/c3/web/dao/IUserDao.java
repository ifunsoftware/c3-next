package org.aphreet.c3.web.dao;

import org.aphreet.c3.web.entity.User;

public interface IUserDao extends ISimpleDao{

	public User getUserByName(String name);

	public User getUserWithMail(String mail);

	public User getUserById(int id);
}