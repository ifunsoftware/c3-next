package org.aphreet.c3.web.util;

import java.util.Collections;
import java.util.Map;

public class SettingsBean {

	private Map<String, String> settings;

	public SettingsBean(Map<String, String> settings){
		this.settings = Collections.unmodifiableMap(settings);
	}
	
	public Map<String, String> getSettings() {
		return settings;
	}
	
	public String get(String key){
		return settings.get(key);
	}
	
	public Integer getInteger(String key){
		Integer result = 0;
		try{
			result = Integer.parseInt(get(key));
		}catch(Exception e){
			
		}
		return result;
	}
	
	public Long getLong(String key){
		Long result = 0l;
		try{
			result = Long.parseLong(get(key));
		}catch(Exception e){
			
		}
		return result;
	}
	
	public Boolean getBoolean(String key){
		Boolean result = false;
		try{
			result = Boolean.parseBoolean(get(key));
		}catch(Exception e){
			
		}
		return result;
	}
	
}
