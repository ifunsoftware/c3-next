package org.aphreet.c3.web.service;

import java.util.List;

import org.aphreet.c3.web.entity.Content;

public interface ISearchService {

	/**
	 * Search resource
	 * @param query - query string
	 * @return
	 */
	public List<Content> searchResources(String query);

}