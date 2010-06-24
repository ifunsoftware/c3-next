package org.aphreet.c3.search.index.filter;

import java.io.IOException;
import java.io.StringReader;
import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.misc.LanguageGuesser;
import org.apache.lucene.misc.TrigramLanguageGuesser;
import org.aphreet.c3.platform.resource.Resource;
import org.aphreet.c3.search.config.SearchConfig;
import org.aphreet.c3.search.index.ResourceHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LanguageGuesserFilter implements ResourceFilter {

	private final Log log = LogFactory.getLog(getClass());
	private LanguageGuesser languageGuesser;
	@Autowired
	private SearchConfig searchConfig;
	
	@PostConstruct
	public void load() throws IOException {
		languageGuesser = new TrigramLanguageGuesser(searchConfig.getTrigramDirectoryPath());
	}

	
	public synchronized void doFilter(Resource resource) {
		String str = null, result = null;
		str = resource.getMetadata().get(ResourceHandler.COMMENT) + resource.getMetadata().get(ResourceHandler.TITLE) 
				+ resource.getMetadata().get(ResourceHandler.CONTENTS);
		if (resource.getMetadata().get(ResourceHandler.LANGUAGE) == null) {
			try {
				result = this.guessLanguage(str);
				resource.getMetadata().put(ResourceHandler.LANGUAGE, result);
			} catch (IOException e) {
				log.error(e, e);
			}
		}
		log.info("Result - " + result);
	}

	@Override
	public boolean supports(Resource resource) {
		return true;//(map.get(Metadata.LANGUAGE) == null);
	}
	
	public synchronized String guessLanguage(String str) throws IOException {
		return languageGuesser.guessLanguage(new StringReader(str));
		
	}

	public void setSearchConfig(SearchConfig searchConfig) {
		this.searchConfig = searchConfig;
	}

}
