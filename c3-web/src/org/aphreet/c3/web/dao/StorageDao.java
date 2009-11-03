package org.aphreet.c3.web.dao;

import org.aphreet.c3.web.entity.Document;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class StorageDao extends SimpleDao implements IStorageDao{

	public boolean isDocumentExists(String ca){
		
		Integer rowCount = (Integer) getSession().createCriteria(Document.class).add(Restrictions.eq("contentAddress", ca)).setProjection(Projections.rowCount()).uniqueResult();
		
		return rowCount != 0;
		
	}
}
