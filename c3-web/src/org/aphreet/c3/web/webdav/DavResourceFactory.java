package org.aphreet.c3.web.webdav;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;

/**
 * Создается сервлетом в ответ на запрос пользователя.
 * Возвращает соответствующие url'у объекты ресурсов 
 * @author coderik
 */
public class DavResourceFactory implements ResourceFactory{
	
	private final Log log = LogFactory.getLog(getClass());
	
	/**
	 * При создании экземпляра класса из контекста спринга 
	 * извлекаются объекты уровня сервисов
	 */
	public DavResourceFactory()
	{
		log.info("Creating resource factory");
	}
	
	/**
	 * По заданному url возвращает соответствующий ресурс
	 * @param host хост на который пришел запрос
	 * @param url указанный в запросе url ресурса
	 * @return объект-обертку для запрошенного ресурса
	 */
	public Resource getResource(String host, String url) //localhost:8080/c3/dav/test
	{
		//log.info("host=" + host + " url=" + url);
		
		String relativeUrl = getRelativeUrl(url);
		
		//log.info("relative url " + relativeUrl);
		
		String groupname = getGroupName(relativeUrl);
		
		if(groupname.isEmpty()){
			//log.info("Creating group list");
			return new DavGroupListResource();
		}
		
		String path = getPath(relativeUrl);
		
		if(path.isEmpty()){
			//log.info("Creating group node");
			return new DavGroupResource(groupname);
		}else{
			//log.info("Creating inode");
			return DavNodeResource.createResource(groupname, path);
		}
	}
	
	/**
	 * зарезервировано для последующего использования
	 * @return "1,2"
	 */
	public String getSupportedLevels() {
		return "1,2";
	}
	
	private String getPath(String url){
		int slashIndex = url.indexOf('/');
		
		if(slashIndex >= 0){
			return url.substring(slashIndex + 1);
		}else{
			return "";
		}
	}
	
	private String getGroupName(String url){
		int slashIndex = url.indexOf('/');
		
		if(slashIndex >= 0){
			return url.substring(0, slashIndex);
		}else{
			return url;
		}
	}
	
	
	private String getRelativeUrl(String url){
		String result = url.replaceAll("//+", "/").substring(url.indexOf("dav") + 3);
		if(!result.isEmpty()){
			if(result.charAt(0) == '/'){
				result = result.substring(1);
			}
		}
		
		if(!result.isEmpty()){
			if(result.charAt(result.length()-1) == '/'){
				result = result.substring(0, result.length()-1);
			}
		}
		
		return result;
	}

}