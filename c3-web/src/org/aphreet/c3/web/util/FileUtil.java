package org.aphreet.c3.web.util;

public class FileUtil {

	public static String getExtension(String name){
		
		int dotIndex = name.lastIndexOf('.');
		
		if(dotIndex >0 && dotIndex < name.length() - 1){
			return name.substring(dotIndex + 1);
		}else{
			return "";
		}
	}
}
