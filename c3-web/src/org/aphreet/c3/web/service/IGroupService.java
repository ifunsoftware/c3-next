package org.aphreet.c3.web.service;

import java.util.List;
import java.util.Set;

import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.entity.WorkGroup;
import org.aphreet.c3.web.exception.DataIsNotAccessibleException;

/**
 *  Service provides groups functionality
 * @author Mikhail Malygin
 *
 */
public interface IGroupService {

	/**
	 * Check group for existence
	 * @param name
	 * @return
	 */
	public boolean isGroupNameExist(String name);
	
	
	public boolean isGroupUrlNameExist(String name);

	/**
	 * Create new Group
	 * @param group
	 */
	public void createGroup(WorkGroup group);

	/**
	 * Returns groups for main page
	 * @return Collection of groups
	 */
	public List<WorkGroup> getTopGroups();

	/**
	 * Find group by name
	 * @param name
	 * @return group instance or null if no group with this name exist
	 */
	public AbstractGroup getGroupByName(String name);

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
	public WorkGroup loadGroup(Integer id, User currentUser)
			throws DataIsNotAccessibleException;

	public WorkGroup getGroupById(Integer id);
	
	/**
	 * Add new member to group
	 * If user a group member already do nothing
	 * @param group
	 * @param name
	 */
	public void addGroupMember(WorkGroup group, String name);

	/**
	 * Remove member from group
	 * @param group
	 * @param id
	 */
	public void deleteGroupMember(WorkGroup group, Integer id);

	/**
	 * Check if user a group member or group owner
	 * @param group
	 * @param user
	 * @return
	 */
	public boolean isUserAGroupMemeber(AbstractGroup group, String name);
	
	
	public Set<AbstractGroup> getGroupsForUser(User user);

}