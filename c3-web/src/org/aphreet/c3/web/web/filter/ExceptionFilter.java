package org.aphreet.c3.web.web.filter;

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

/**
 * In case of uncaught exceptions filter redirects to error 500 page
 * @author aphreet
 *
 */
public class ExceptionFilter implements Filter {

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
			e.printStackTrace();
			sendRedirect(request, response, "/error/500.xhtml");
			
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
	
	private void sendRedirect(ServletRequest request, ServletResponse response, String target){
		response.reset();
		try {
			((HttpServletResponse) response)
				.sendRedirect(((HttpServletRequest) request).getContextPath() + target);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}