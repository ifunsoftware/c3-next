package org.aphreet.c3.web.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.aphreet.c3.web.dao.IGroupDao;
import org.aphreet.c3.web.dao.IUserDao;
import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.entity.WorkGroup;
import org.aphreet.c3.web.exception.DataIsNotAccessibleException;
import org.aphreet.c3.web.service.IGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *  Service provides groups functionality
 * @author Mikhail Malygin
 *
 */
@Service
public class GroupService implements IGroupService{

	@Autowired
	private IGroupDao groupDao;
	
	@Autowired
	private IUserDao userDao;
	
	
	/**
	 * Check group for existence
	 * @param name
	 * @return
	 */
	public boolean isGroupNameExist(String name){
		return groupDao.isGroupNameExist(name);
	}
	

	public boolean isGroupUrlNameExist(String name){
		return groupDao.isGroupUrlNameExist(name);
	}
	
	/**
	 * Create new Group
	 * @param group
	 */
	public void createGroup(WorkGroup group){
		groupDao.persist(group);
	}
	
	/**
	 * Returns groups for main page
	 * @return Collection of groups
	 */
	public List<WorkGroup> getTopGroups(){
		return groupDao.getGroups(10);
	}
	
	/**
	 * Find group by name
	 * @param name
	 * @return group instance or null if no group with this name exist
	 */
	public AbstractGroup getGroupByName(String name)
	{
		return groupDao.getGroupByName(name);
	}
	
	
	/**
	 * Loads group for user.
	 * If no group with specified id exists or user is not a member
	 * of this group throws {@link DataIsNotAccessibleException}
	 * 
	 * @param id
	 * @param currentUser
	 * @return
	 * @throws DataIsNotAccessibleException
	 */
	public WorkGroup loadGroup(Integer id, User currentUser) throws DataIsNotAccessibleException{
		if(id != null){
			WorkGroup group = (WorkGroup) groupDao.getEntity(id, WorkGroup.class);
			if(group != null){
				if(isUserAGroupMemeber(group, currentUser)){
					return group;
				}
			}
		}
		throw new DataIsNotAccessibleException();
	}
	
	
	/**
	 * Add new member to group
	 * If user a group member already do nothing
	 * @param group
	 * @param name
	 */
	public void addGroupMember(WorkGroup group, String name){
		User userToAdd = userDao.getUserByName(name);
		
		if(userToAdd != null){
			if(!isUserAGroupMemeber(group, userToAdd)){
				group.getMembers().add(userToAdd);
			}
		}
	}
	
	/**
	 * Remove member from group
	 * @param group
	 * @param id
	 */
	public void deleteGroupMember(WorkGroup group, Integer id){
		User userToDelete = (User) userDao.getEntity(id, User.class);
		group.getMembers().remove(userToDelete);
	}
	
	private boolean isUserAGroupMemeber(AbstractGroup group, User user){
		if(user == null || group == null){
			return false;
		}
		return group.isMember(user);
	}
	
	/**
	 * Check if user a group member or group owner
	 * @param group
	 * @param user
	 * @return
	 */
	public boolean isUserAGroupMemeber(AbstractGroup group, String name){
		User userToAdd = userDao.getUserByName(name);
		return isUserAGroupMemeber(group, userToAdd);
	}

	public void setGroupDao(IGroupDao groupDao) {
		this.groupDao = groupDao;
	}

	public void setUserDao(IUserDao userDao) {
		this.userDao = userDao;
	}


	@Override
	public Set<AbstractGroup> getGroupsForUser(User user) {
		
		TreeSet<AbstractGroup> result = new TreeSet<AbstractGroup>(new Comparator<AbstractGroup>() {

			@Override public int compare(AbstractGroup o1, AbstractGroup o2){
				return o1.getName().compareTo(o2.getName());
			}
		});
		
		result.addAll(groupDao.getGroupsWhereUserIsMember(user));
		result.addAll(groupDao.getGroupsWhereUserIsOwner(user));
		
		return result;
	}


	@Override
	public WorkGroup getGroupById(Integer id) {
		return (WorkGroup) groupDao.getEntity(id, WorkGroup.class);
	}
}
