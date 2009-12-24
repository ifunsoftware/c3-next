package org.aphreet.c3.search.index.filter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.platform.resource.Resource;
import org.aphreet.c3.search.config.SearchConfig;
import org.aphreet.c3.search.index.ResourceHandler;
import org.aphreet.c3.search.index.server.TextExtractorProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TextExtractorFilter implements ResourceFilter {

	@Autowired
	private TextExtractorProvider textExtractorProvider;
	
	@Autowired
	private SearchConfig searchConfig;
	
	private final Log log = LogFactory.getLog(getClass());

	@Override
	public boolean supports(Resource resource) {
		boolean supports = true;
/*		String resourceClass = map.get(Metadata.CLASS);
		if (resourceClass.equals(Document.class.getCanonicalName())) {
			supports = true;
		}
*/		return supports;
	}

	public void doFilter(Resource resource) {
		
		String address = resource.address();
		
		File tmpFile = null;
		
		try{
			tmpFile = File.createTempFile(address, null,
					 new File(searchConfig.getHomeDirectory()));
			FileOutputStream outputStream = new FileOutputStream(tmpFile);
			int last = resource.getVersions().size();
			outputStream.write(resource.getVersions().get(last-1).data().getBytes());
			outputStream.flush();
			outputStream.close();
			
			Map <String, String> tikasMetadata = 
				textExtractorProvider.extract(tmpFile.getAbsolutePath());


			for (String name : tikasMetadata.keySet()) {
				if (resource.getMetadata().get(name) == null)
					resource.getMetadata().put(name, tikasMetadata.get(name));
			}
			
			resource.getMetadata().put(ResourceHandler.CONTENTS, tikasMetadata.get(ResourceHandler.CONTENTS));
			resource.getMetadata().put(ResourceHandler.REVISION, String.valueOf(last));
			log.info("Content from " + resource.address() + "extracted");
		}catch(IOException e){
			log.error("Failed to process filter due to exception", e);
		}finally{
			if(tmpFile != null && tmpFile.exists()){
				tmpFile.delete();
			}
		}
		
	}

	public void setTextExtractorProvider(TextExtractorProvider provider) {
		this.textExtractorProvider = provider;
	}

	public void setSearchConfig(SearchConfig searchConfig) {
		this.searchConfig = searchConfig;
	}

}
