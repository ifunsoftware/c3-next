package org.aphreet.c3.search.index.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.search.config.SearchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.springframework.stereotype.Component;

public class TextExtractorProvider implements SearchProvider {

	private final Log logger = LogFactory.getLog(getClass()); 

	private String serverFileName;
	private String serviceUrl;
	private String serviceInterfaceName;
	private String javaCommand;
	
	private Process process;
	private SearchProvider textExtractorInterface;
	
	private SearchConfig searchConfig;
	
	public Map<String,String> extract(String fileName) {
		Map<String, String> map = null;
		try {
			map = textExtractorInterface.extract(fileName);
		} catch (Throwable e) {
			logger.info("tika-server is missing. Try to restart");
			process.destroy();
			startRemoteTextExtractor();
			logger.info("tika-server restart succeed!!");
		}
		return map;
	}
	
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}
	
	public void setServiceInterfaceName(String serviceInterfaceName) {
		this.serviceInterfaceName = serviceInterfaceName;
	}
	
	public void setSearchConfig(SearchConfig searchConfig) {
		this.searchConfig = searchConfig;
	}

	@SuppressWarnings("unused")
	@PostConstruct
	private void setTikaServerFileName() {
		this.javaCommand = "java"/*settingsBean.get("javaCommand")*/;
		this.serverFileName = searchConfig.getTikaServerPath();
		startRemoteTextExtractor();
	}
	
	protected void startRemoteTextExtractor() {
		try {
			logger.info("Starting tika server");
			logger.info("Using java command: " + javaCommand);
			ProcessBuilder processBuilder = new ProcessBuilder(javaCommand, "-jar", serverFileName);
			processBuilder.directory(new File(serverFileName).getParentFile());
			processBuilder.redirectErrorStream(true);
			process = processBuilder.start();	

			BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String str = stdoutReader.readLine();
			while (!str.contains("Tika server started")) {
				logger.info(str);
				str = stdoutReader.readLine();
			}
			logger.info(str);
			stdoutReader.close();

			RmiProxyFactoryBean rmiBean = new RmiProxyFactoryBean();
			rmiBean.setServiceInterface(Class.forName(serviceInterfaceName));
			rmiBean.setServiceUrl(serviceUrl);
			rmiBean.afterPropertiesSet();
			textExtractorInterface = (SearchProvider) rmiBean.getObject();
		} catch (Exception e) {
			logger.error("tika-server fell down ultimately", e);
		}
	}
	
	@PreDestroy
	protected void destroy() {
		process.destroy();
	}

}
