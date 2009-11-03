package org.aphreet.c3.web.util;

import java.io.IOException;
import java.net.URLEncoder;

public class PathBuilder {
	
	private StringBuilder urlBuilder = new StringBuilder();
	
	boolean hasParameters = false;
	
	public PathBuilder(String url){
		urlBuilder.append(url);
	}
	
	public PathBuilder addParam(String name, Object value){
		try{
			String delimiter;
			
			if(hasParameters){
				delimiter = "&";
			}else{
				delimiter = "?";
				hasParameters = true;
			}
			
			urlBuilder.append(delimiter)
				.append(name).append("=")
				.append(URLEncoder.encode(value.toString(), "UTF-8"));
			
		}catch(IOException e){
			e.printStackTrace();
		}
		return this;
	}
	
	public String toString(){
		return urlBuilder.toString();
	}
}
