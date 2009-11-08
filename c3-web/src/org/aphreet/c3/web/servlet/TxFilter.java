package org.aphreet.c3.web.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.context.ContextLoader;

/**
 * Filter create new transaction on request an commit it in the end of request procession
 * In case of uncaught exceptions performs tx rollback
 * @author aphreet
 *
 */
public class TxFilter implements Filter{

	private PlatformTransactionManager txManager;
	
	private Log logger = LogFactory.getLog(getClass());
	
	private String a4jPrefix;
	
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		
		String uri = httpRequest.getRequestURI();
		//we do not need tx's on a4j resources
		if(!uri.startsWith(a4jPrefix)){
			
			DefaultTransactionDefinition def = new DefaultTransactionDefinition();
			def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
			TransactionStatus status = txManager.getTransaction(def);
			logger.debug("tx created");
			
			try{
				chain.doFilter(request, response);
				txManager.commit(status);
				logger.debug("tx commited");
			}catch(ServletException e){
				txManager.rollback(status);
				logger.debug("tx rolledback");
				throw e;
			}
		}else{
			chain.doFilter(request, response);
		}
		
		
		
	}

	public void init(FilterConfig arg0) throws ServletException {
		AbstractApplicationContext context = (AbstractApplicationContext) ContextLoader.getCurrentWebApplicationContext();
		txManager = (PlatformTransactionManager) context.getBean("txManager");
	
		a4jPrefix = arg0.getServletContext().getContextPath() + "/a4j/";
	}
	
	public void destroy() {
		
	}

}
