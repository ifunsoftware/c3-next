package org.aphreet.c3.web.dao;

import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.WikiPage;
import org.aphreet.c3.web.entity.WikiPageVersion;
import org.springframework.stereotype.Repository;

@Repository
public class WikiDao extends SimpleDao{

	public WikiPage getPage(AbstractGroup group, String name){
		
		return (WikiPage) top(getSession().createQuery("from org.aphreet.c3.web.entity.WikiPage page where " +
				"page.title = :name and page.group.id = :id")
				.setString("name", name).setInteger("id", group.getId()).list());
	}
	
	public WikiPageVersion getPageVersion(WikiPage page, Integer revision){
		return (WikiPageVersion) top(getSession()
				.createQuery("from org.aphreet.c3.web.entity.WikiPageVersion vers" +
						" where vers.number = :number and vers.page.id = :id ")
						.setInteger("number", revision).setInteger("id", page.getId()).list());
	}
}
