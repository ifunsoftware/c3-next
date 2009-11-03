package org.aphreet.c3.web.dao;

import java.util.List;

import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.entity.WorkGroup;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

@Repository
public class GroupDao extends SimpleDao implements IGroupDao{

	/* (non-Javadoc)
	 * @see org.aphreet.c3.web.dao.IGroupDao#isGroupNameExist(java.lang.String)
	 */
	public boolean isGroupNameExist(String name){
		return !getSession().createCriteria(AbstractGroup.class).add(Restrictions.eq("name", name)).list().isEmpty();
	}
	

	public boolean isGroupUrlNameExist(String name){
		return !getSession().createCriteria(AbstractGroup.class).add(Restrictions.eq("urlName", name)).list().isEmpty();
	}
	
	/* (non-Javadoc)
	 * @see org.aphreet.c3.web.dao.IGroupDao#getGroupByName(java.lang.String)
	 */
	public AbstractGroup getGroupByName(String name){
		return (AbstractGroup) top(getSession().createCriteria(AbstractGroup.class)
							.add(Restrictions.eq("urlName", name)).list());
		
	}
	
	/* (non-Javadoc)
	 * @see org.aphreet.c3.web.dao.IGroupDao#getGroups(int)
	 */
	@SuppressWarnings("unchecked")
	public List<WorkGroup> getGroups(int count) {

		Criteria criteria = getSession().createCriteria(WorkGroup.class)
				.addOrder(Order.asc("name"));
		if (count > 0) {
			criteria.setMaxResults(count);
		}
		return criteria.list();
	}
	
	@SuppressWarnings("unchecked")
	public List<WorkGroup> getGroupsWhereUserIsMember(User user){
		return getSession().createCriteria(WorkGroup.class).createCriteria("members").add(Restrictions.idEq(user.getId())).list();
	}


	@SuppressWarnings("unchecked")
	@Override
	public List<AbstractGroup> getGroupsWhereUserIsOwner(User user) {
		return getSession().getNamedQuery("groups_where_user_is_owner").setInteger("user_id", user.getId()).list();
	}

}
