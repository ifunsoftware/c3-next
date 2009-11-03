package org.aphreet.c3.web.dao;

import java.util.List;

import org.aphreet.c3.web.entity.User;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;


@Repository
public class UserDao extends SimpleDao implements IUserDao{
	
	/* (non-Javadoc)
	 * @see org.aphreet.c3.web.dao.IUserDao#list()
	 */
	public List<?> list(){
		return getSession().createCriteria(User.class)
				.addOrder(Order.asc("id")).list();
	}
	
	/* (non-Javadoc)
	 * @see org.aphreet.c3.web.dao.IUserDao#getUserByName(java.lang.String)
	 */
	public User getUserByName(String name){
		return (User) top(getSession().createCriteria(User.class)
							.add(Restrictions.eq("name", name)).list());
		
	}
	
	/* (non-Javadoc)
	 * @see org.aphreet.c3.web.dao.IUserDao#getUserWithMail(java.lang.String)
	 */
	public User getUserWithMail(String mail){
		return (User) top(getSession().createCriteria(User.class)
							.add(Restrictions.eq("mail", mail)).list());
		
	}

	@Override
	public User getUserById(int id) {
		return (User) getEntity(id, User.class);
	}
}