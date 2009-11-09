package org.aphreet.c3.web.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.platform.resource.*;
import org.aphreet.c3.web.entity.Document;
import org.aphreet.c3.web.entity.DocumentVersion;
import org.aphreet.c3.web.service.IResourceService;
import org.aphreet.c3.web.util.SpringUtil;

/**
 * Servlet implementation class FileServlet
 */
public class FileServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Log logger = LogFactory.getLog(getClass());

	private IResourceService resourceService;

	public void init(){
		resourceService = (IResourceService) SpringUtil.getBean("resourceService");
		if(resourceService == null){
			throw new NullPointerException("file bean can't be null");
		}
	}

	public void service(HttpServletRequest request, HttpServletResponse response){
		String idStr = request.getParameter("id");
		String revStr = request.getParameter("rev");
		
		Integer rev = null;
		
		try{
			rev = Integer.parseInt(revStr);
		}catch (Exception e) {
		}


		Document doc = resourceService.getDocumentWithCa(idStr);


		if(doc != null){
			try {
				// Init servlet response.
				response.reset();
				
				int length = (int) doc.getHeadVersion().getSize();
				
				response.setContentLength(length);
				response.setContentType(doc.getContentType());
				response.setHeader("Content-disposition", "inline; filename=\""
						+ doc.getName() + "." + doc.getExtension() + "\"");

				OutputStream out = response.getOutputStream();
			
				DataWrapper data;
				
				List<DocumentVersion> versions = doc.getVersions();
				
				if(rev != null){
					
					if(rev <= versions.size()-1){
						data = versions.get(rev).getData();
					}else{
						data = doc.getHeadVersion().getData();
					}
				}else{
					data = doc.getHeadVersion().getData();
				}
				
				data.writeTo(out);
				
				
				out.flush();

			} catch (IllegalStateException e) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			} catch (SocketException e) {
				logger.warn("socket exception :" + e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
