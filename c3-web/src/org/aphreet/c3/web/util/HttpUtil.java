package org.aphreet.c3.web.util;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class HttpUtil {

	private static FacesContext getFacesContext(){
		return FacesContext.getCurrentInstance();
	}
	
	private static HttpServletRequest getRequest(){
		return (HttpServletRequest) getFacesContext()
			.getExternalContext().getRequest();
	}
	
	private static HttpServletResponse getResponse(){
		return (HttpServletResponse) getFacesContext()
			.getExternalContext().getResponse();
	}

	public static void sendNotFound(){
		sendRedirect("/error/404.xhtml");
	}
	
	public static void sendAccessDenied(){
		sendRedirect("/error/403.xhtml");
	}

	public static void sendServerError(){
		sendRedirect("/error/500.xhtml");
	}
	
	public static void sendRedirect(String path){
		try {
			getResponse().sendRedirect(getRequest().getContextPath() + path);
			getFacesContext().responseComplete();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String getParameter(String key){
		return getFacesContext().getExternalContext().getRequestParameterMap().get(key);
	}
	
	public static String getAbsoluteUrl(){
		HttpServletRequest request = getRequest();
		
		String scheme = request.getScheme();
		
		String serverName = request.getServerName();
		int serverPort = request.getServerPort();
		if(serverPort != 80){
			serverName = serverName + ":" + serverPort;
		}
		String contextPath = request.getContextPath();
		if(!contextPath.equals("/")){
			contextPath = contextPath + "/";
		}
		
		String absoluteUrl = scheme + "://" + serverName + contextPath;
		return absoluteUrl;
	}
	
	public static HttpSession getSession(){
		return getRequest().getSession();
	}
}
