package org.aphreet.c3.web.dao;

import java.util.List;

import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.entity.WorkGroup;

public interface IGroupDao extends ISimpleDao{

	public boolean isGroupNameExist(String name);
	
	public boolean isGroupUrlNameExist(String name);
	
	public AbstractGroup getGroupByName(String name);

	public List<WorkGroup> getGroups(int count);

	public List<WorkGroup> getGroupsWhereUserIsMember(User user);
	
	public List<AbstractGroup> getGroupsWhereUserIsOwner(User user);
}