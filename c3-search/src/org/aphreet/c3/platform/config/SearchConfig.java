package org.aphreet.c3.search.config;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.aphreet.c3.platform.common.Constants;
import org.aphreet.c3.platform.management.PlatformManagementEndpoint;
import org.aphreet.c3.platform.management.PlatformPropertyListener;
import org.aphreet.c3.platform.management.PropertyChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SearchConfig implements PlatformPropertyListener {
	
	public static final String INDEX_DIRECTORY_PATH = "c3.search.index.directory";
	public static final String TRIGRAM_DIRECTORY_PATH = "c3.search.trigram.directory";
	public static final String TIKA_SERVER_PATH = "c3.search.trigram.directory";

	public static final String DEFAULT_INDEX_DIRECTORY_NAME = "/index";
	public static final String DEFAULT_TRIGRAM_DIRECTORY_NAME = "/trigrams";
	public static final String DEFAULT_TIKA_SERVER_NAME = "/tika-server.jar";

	public static final int INITIAL_CORE_POOL_SIZE = 2;
	public static final int INITIAL_MAXIMUM_POOL_SIZE = 3;
	public static final long INITIAL_KEEP_ALIVE_TIME = 30L;
	public static final long INITIAL_RAM_BUFFER_SIZE = 134217728L; // 128 Mb
	public static final String INDEXED = "indexed";
	
	private String indexDirectoryPath;
	private String trigramsDirectoryPath;
	private String tikaServerPath;
	
	private String c3Home;
	
	@Autowired
	private PlatformManagementEndpoint managementEndpoint;

	@Override
	public String[] listeningForProperties() {
		return new String [] {SearchConfig.INDEX_DIRECTORY_PATH};
	}

	@Override
	public void propertyChanged(PropertyChangeEvent event) {
		if (event.name().equals(SearchConfig.INDEX_DIRECTORY_PATH)) {
			//indexDirectoryPath = event.newValue();
			// TODO process index directory change
		}
		if (event.name().equals(SearchConfig.TRIGRAM_DIRECTORY_PATH)) {
			//trigramsDirectoryPath = event.newValue();
			// TODO process trigram directory change
		}
		if (event.name().equals(SearchConfig.TIKA_SERVER_PATH)) {
			//tikaServerPath = event.newValue();
			// TODO process tika server change
		}
	}

	@PostConstruct
	public void init() {
		managementEndpoint.registerPropertyListener(this);
		c3Home = managementEndpoint.getPlatformProperties().get(Constants.C3_PLATFORM_HOME());
	}

	@PreDestroy
	public void destroy() {
		managementEndpoint.unregisterPropertyListener(this);
	}
	
	public String getIndexDirectoryPath() {
		if (indexDirectoryPath == null)
			return c3Home + "/index";
		else 
			return indexDirectoryPath;
	}

	public String getTrigramDirectoryPath() {
		if (trigramsDirectoryPath == null)
			return c3Home + "/trigrams";
		else 
			return trigramsDirectoryPath;
	}
	
	public String getTikaServerPath() {
		if (tikaServerPath == null)
			return c3Home + "/tika-server.jar";
		else 
			return tikaServerPath;
	}

	public String getHomeDirectory() {
		return c3Home;
	}

	public PlatformManagementEndpoint getManagementEndpoint() {
		return managementEndpoint;
	}

	public void setManagementEndpoint(PlatformManagementEndpoint endpoint) {
		this.managementEndpoint = endpoint;
	}

}
