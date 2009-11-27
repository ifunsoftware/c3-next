package org.aphreet.c3.web.servlet;

import java.io.IOException;

import javax.faces.application.ViewExpiredException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * In case of uncaught exceptions filter redirects to error 500 page
 * @author aphreet
 *
 */
public class ExceptionFilter implements Filter {

	private final Log logger = LogFactory.getLog(getClass());
	
	public void init(FilterConfig arg0) throws ServletException {

	}
	
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		
		try{
			chain.doFilter(request, response);
		}catch(ServletException e){
			
			Throwable cause = e.getCause();
			
			if(cause != null){
				if(cause instanceof ViewExpiredException){
					redirectToTarget(request, response);
					return;
				}
			}
			logger.error(e);
			throw e;
		}
		
	}

	public void destroy() {

	}
	
	private void redirectToTarget(ServletRequest request, ServletResponse response){
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		
		resp.reset();
		try {
			resp.sendRedirect(req.getRequestURI());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
