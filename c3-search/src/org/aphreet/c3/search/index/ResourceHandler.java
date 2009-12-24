package org.aphreet.c3.search.index;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.aphreet.c3.platform.resource.Resource;
import org.aphreet.c3.search.index.filter.ResourceFilter;
import org.springframework.stereotype.Component;

/**
 * Предварительная обработка документа перед индексированием.
 * @author Ildar Ashirbaev
 */
@Component
public class ResourceHandler {
	
	public static final String ENGLISH_LANGUAGE = "en";
	public static final String RUSSIAN_LANGUAGE = "ru";
	public static final String ADDRESS = "address";
	public static final String REVISION = "revision";
	public static final String CONTENT = "content";
	public static final String TITLE = "title";
	public static final String COMMENT = "comment";
	public static final String TAGS = "tags";
	public static final String AUTHOR = "author";
	public static final String CONTENTS = "contents";
	public static final String LANGUAGE = "language";
	public static final String CREATOR = "creator";
	public static final String SUBJECT = "subject";
	public static final String KEYWORDS = "keywords";
	public static final String DESCRIPTION = "description";

	private List<ResourceFilter> resourceFilterList;

	@SuppressWarnings("unused")
	private final Log log = LogFactory.getLog(getClass());
	
	/**
	 * Пропускает ресурс через цепочку фильтров, составляет объект класса 
	 * <code><a href="http://lucene.apache.org/java/2_2_0/api/org/apache/lucene/document/Document.html">Document</a></code>,
	 * предназначенного для индексирования.
	 * @param resource метаданные ресурса
	 * @return <code><a href="http://lucene.apache.org/java/2_2_0/api/org/apache/lucene/document/Document.html">Document</a></code> 
	 */
	public Document process(Resource resource) {
		for (ResourceFilter filter : resourceFilterList) {
			if (filter.supports(resource)) {
				filter.doFilter(resource);
			}
		}
		log.info("Create document for resource " + resource.address());
		Document doc = new Document();
		for (String key : resource.getMetadata().keySet()) {
			String value = resource.getMetadata().get(key);

			if (key.equals(ResourceHandler.ADDRESS) || key.equals(ResourceHandler.REVISION)) {
				doc.add(new Field(key, value, Field.Store.YES, Field.Index.NOT_ANALYZED));

			} else if (key.equals(ResourceHandler.TITLE) || key.equals(ResourceHandler.COMMENT) || 
					key.equals(ResourceHandler.AUTHOR) || key.equals(ResourceHandler.TAGS)) {
				Field field = new Field(key, value, Field.Store.NO, Field.Index.ANALYZED);
				field.setBoost(2.0f);
				doc.add(field);

			} else {
				doc.add(new Field(key, value, Field.Store.NO, Field.Index.ANALYZED));
			}
			
		}
		return doc;
	}

	/**
	 * Устанавливает цепочку фильтров.
	 * @param resourceFilterList список фильтров.
	 */
	public void setResourceFilterList(List<ResourceFilter> resourceFilterList) {
		this.resourceFilterList = resourceFilterList;
	}

}
