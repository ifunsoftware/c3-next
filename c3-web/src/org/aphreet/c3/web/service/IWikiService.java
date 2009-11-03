package org.aphreet.c3.web.service;

import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.entity.WikiPage;
import org.aphreet.c3.web.entity.WikiPageVersion;

public interface IWikiService {

	/**
	 * Find page with specified name in group namespace
	 * @param group
	 * @param name
	 * @return
	 */
	public WikiPage getPage(AbstractGroup group, String name);

	/**
	 * Get content of page in revision
	 * @param page
	 * @param revision
	 * @return
	 */
	public WikiPageVersion getPageVersion(WikiPage page, Integer revision);

	/**
	 * Updates wiki page
	 * Each update crates new page revision
	 * @param page
	 * @param text
	 * @param editor
	 */
	public void updatePage(WikiPage page, String text, User editor, boolean notify);

	/**
	 * Craete new page
	 * @param page
	 * @param text
	 */
	public void createPage(WikiPage page, String text, boolean notify);

	public String parseWikiText(String input, final AbstractGroup group);
	
	public void deletePage(WikiPage page);


}