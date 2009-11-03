package org.aphreet.c3.web.dao;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.web.entity.Entity;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class SimpleDao implements ISimpleDao {

	@Autowired
	protected SessionFactory sf;
	
	@SuppressWarnings("unused")
	private final Log logger = LogFactory.getLog(getClass());
	
	protected final Session getSession(){
		Session session = sf.getCurrentSession();
		return session;
	}
	
	public final void initialize(Object object){
		Hibernate.initialize(object);
	}
	
	/* (non-Javadoc)
	 * @see org.aphreet.c3.web.dao.ISimpleDao#persist(org.aphreet.c3.web.entity.Entity)
	 */
	public void persist(Entity e){
		getSession().persist(e);
	}
	
	public void saveOrUpdate(Entity e){
		getSession().saveOrUpdate(e);
	}
	
	/* (non-Javadoc)
	 * @see org.aphreet.c3.web.dao.ISimpleDao#getEntity(java.io.Serializable, java.lang.Class)
	 */
	public Entity getEntity(Serializable key, Class<? extends Entity> clazz){
		if(key == null){
			return null;
		}
		return (Entity) getSession().get(clazz, key);
	}
	
	public void merge(Entity e){
		getSession().merge(e);
	}
	
	protected Object top(List<?> list){
		if(list.isEmpty()){
			return null;
		}else{
			return list.get(0);
		}
	}

	public void delete(Entity e) {
		getSession().delete(e);
	}
}
