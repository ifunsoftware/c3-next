package org.aphreet.c3.web.service;

import java.util.Map;

public interface IConfigService {

	String MAIL_ENABLED = "c3.web.mail.enabled";
	String MAIL_DOMAIN = "c3.web.mail.domain";
	
	String getProperty(String key);
	
	Boolean getBoolProperty(String key);
	
	void setProperty(String key, String value);
	
	Boolean isMailEnabled();
	
	String getMailDomain();
	
	Map<String, String> getProperties();
}
