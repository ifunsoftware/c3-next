package org.aphreet.c3.web.webbeans;

import org.aphreet.c3.web.util.HttpUtil;
import org.springframework.stereotype.Component;

@Component
public class AppBean {

	private String version;
	
	public String getAppBasePath(){
		return HttpUtil.getAbsoluteUrl();
	}
	
	public void setVersion(String version){
		this.version = version;
	}
	
	public String getVersion(){
		return version;
	}
}
