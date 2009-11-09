package org.aphreet.c3.web.service.impl;

import java.util.Map;

import org.aphreet.c3.platform.management.PlatformManagementEndpoint;
import org.aphreet.c3.web.service.IConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigService implements IConfigService{

	@Autowired
	private PlatformManagementEndpoint platformManagementEndpoint;
	
	public void setPlatformAccessEndpoint(
			PlatformManagementEndpoint platformManagementEndpoint) {
		this.platformManagementEndpoint = platformManagementEndpoint;
	}
	
	@Override
	public String getProperty(String key) {
		return getProperties().get(key);
	}
	
	public Boolean getBoolProperty(String key){
		try{
			return Boolean.parseBoolean(getProperty(key));
		}catch(Throwable e){
			return false;
		}
	}

	@Override
	public void setProperty(String key, String value) {
		platformManagementEndpoint.setPlatformProperty(key, value);
	}
	

	public Boolean isMailEnabled(){
		return getBoolProperty(MAIL_ENABLED);
	}
	
	public String getMailDomain(){
		return getProperty(MAIL_DOMAIN);
	}
	
	public Map<String, String> getProperties(){
		return platformManagementEndpoint.getPlatformProperties();
	}

}
