package org.aphreet.c3.web.dao;

import java.util.List;

import org.aphreet.c3.web.entity.Content;
import org.hibernate.criterion.Order;
import org.springframework.stereotype.Repository;

@Repository
public class SearchDao extends SimpleDao{

	@SuppressWarnings("unchecked")
	public List<Content> loadFoundResourcesStub(){
		return getSession().createCriteria(Content.class)
			.addOrder(Order.asc("id")).setMaxResults(10).list();
	}
}
