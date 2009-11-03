package org.aphreet.c3.web.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserProfile implements Entity{

	private static final long serialVersionUID = -5522426637538540240L;

	public static final String NOTIFY_PROP = "notify-me";
	
	private int id;
	
	private String name;
	
	private String family;
	
	private Date birthDate;
	
	private String info;
	
	private String im;

	private Map<String, String> settings;
	
	private User user;

	public UserProfile(){
		settings = new HashMap<String, String>();
	}
	
	public String getSetting(String key){
		if(settings != null){
			return settings.get(key);
		}else return null;
	}
	
	public Integer getIntSetting(String key){
		Integer result = 0;
		try{
			result = Integer.parseInt(settings.get(key));
		}catch(Exception e){
			
		}
		return result;
	}
	
	public Boolean getBoolSetting(String key){
		Boolean result = false;
		try{
			result = Boolean.parseBoolean(settings.get(key));
		}catch(Exception e){
			
		}
		return result;
	}
	
	public void addSetting(String key, Integer value){
		this.settings.put(key, value.toString());
	}
	
	public void addSetting(String key, String value){
		this.settings.put(key, value);
	}
	
	public void addSetting(String key, Boolean value){
		this.settings.put(key, value.toString());
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFamily() {
		return family;
	}

	public void setFamily(String family) {
		this.family = family;
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Map<String, String> getSettings() {
		return settings;
	}

	public void setSettings(Map<String, String> settings) {
		this.settings = settings;
	}
	
	public String getIm() {
		return im;
	}

	public void setIm(String im) {
		this.im = im;
	}
}
