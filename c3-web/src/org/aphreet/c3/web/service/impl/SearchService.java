package org.aphreet.c3.web.service.impl;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.web.entity.Content;
import org.aphreet.c3.web.service.ISearchService;
import org.springframework.stereotype.Service;

/**
 * Service provides search functions (add/remove from index, search)
 * @author Mikhail Malygin
 *
 */

@Service
public class SearchService implements ISearchService {
	
	private static Log logger = LogFactory.getLog(SearchService.class);
	
	/**
	 * Search resource
	 * @param query - query string
	 * @return
	 */
	public List<Content> searchResources(String query){
		//TODO implement search
		
		return null;
//		List<Resource> list = null;
//		try {
//			list = searcher.search(query);
//		} catch (Exception e) {
//			logger.warn(this, e);
//		}
//		return list;
	}

}
