package org.aphreet.c3.search.index.filter;

import java.util.Map;

import org.aphreet.c3.platform.resource.Resource;

/**
 * Base interface to be implemented by filter instances
 * @author Ildar Ashirbaev
 *
 */
public interface ResourceFilter {
	
	/**
	 * При обработке ресурса вначале необходимо провести 
	 * проверку поддерживается ли данный ресурс данным 
	 * <code>ResouceFiler</code>. 
	 * @param resource resource metadata
	 * @return true if filter supports resource
	 */
	boolean supports(Resource resource);

	/**
	 * Предначен для обработки ресурса. 
	 * @param resource метаданные ресурса входные
	 * @return метаданные ресурса выходные
	 */
	void doFilter(Resource resource);

}
