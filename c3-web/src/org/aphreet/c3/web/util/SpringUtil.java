package org.aphreet.c3.web.util;

import java.util.Locale;

import javax.faces.context.FacesContext;

import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class SpringUtil {

	private static XmlWebApplicationContext context = 
		(XmlWebApplicationContext) ContextLoader.getCurrentWebApplicationContext();
	
	
	public static Object getBean(String name){
		return context.getBean(name);
	}
	
	public static Locale getCurrentLocale(){
		
		Locale locale;
		try{
			locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
		}catch(Exception e){
			locale = Locale.US;
		}
		return locale;
	}
}
