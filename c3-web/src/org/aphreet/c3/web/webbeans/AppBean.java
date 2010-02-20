package org.aphreet.c3.web.webbeans;

import org.aphreet.c3.web.util.HttpUtil;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.stereotype.Component;
import org.osgi.framework.BundleContext;

@Component
public class AppBean implements BundleContextAware{

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

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        version = (String) bundleContext.getBundle().getHeaders().get("Bundle-Version");
    }
}
