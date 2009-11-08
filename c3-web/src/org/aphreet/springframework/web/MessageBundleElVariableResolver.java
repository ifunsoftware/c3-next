package org.aphreet.springframework.web;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.Locale;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotWritableException;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.context.WebApplicationContext;

public class MessageBundleElVariableResolver extends ELResolver {

	private final Log logger = LogFactory.getLog(getClass());
	
	protected WebApplicationContext getWebApplicationContext(ELContext elContext) {
		return (WebApplicationContext) ((ServletContext) FacesContext
				.getCurrentInstance().getExternalContext().getContext())
				.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
	}

	@Override
	public Object getValue(ELContext elContext, Object base, Object property)
			throws ELException {
		if (base == null) {
			String beanName = property.toString();
			BeanFactory bf = getBeanFactory(elContext);
			if (bf.containsBean(beanName)) {
				if (logger.isTraceEnabled()) {
					logger.trace("Successfully resolved variable '" + beanName
							+ "' in Spring BeanFactory");
				}
				elContext.setPropertyResolved(true);
				return bf.getBean(beanName);
			}
		} else {
			if (base instanceof ResourceBundleMessageSource) {
				ResourceBundleMessageSource resources = (ResourceBundleMessageSource) base;

				Locale locale = (Locale) ((HttpServletRequest) FacesContext
						.getCurrentInstance().getExternalContext().getRequest())
						.getSession().getAttribute("CURRENT_LOCALE");
				if (locale == null) {
					locale = FacesContext.getCurrentInstance().getViewRoot()
							.getLocale();
				}

				elContext.setPropertyResolved(true);
				return resources.getMessage(property.toString(), null, locale);
			}
		}
		return null;
	}
	
	@Override
 	public Class<?> getType(ELContext elContext, Object base, Object property) throws ELException {
   		if (base == null) {
   			String beanName = property.toString();
   			BeanFactory bf = getBeanFactory(elContext);
   			if (bf.containsBean(beanName)) {
   				elContext.setPropertyResolved(true);
   				return bf.getType(beanName);
   			}
   		}
   		return null;
   	}
   
   	@Override
   	public void setValue(ELContext elContext, Object base, Object property, Object value) throws ELException {
   		if (base == null) {
   			String beanName = property.toString();
   			BeanFactory bf = getBeanFactory(elContext);
   			if (bf.containsBean(beanName)) {
   				throw new PropertyNotWritableException(
   						"Variable '" + beanName + "' refers to a Spring bean which by definition is not writable");
   			}
   		}
   	}
   
   	@Override
   	public boolean isReadOnly(ELContext elContext, Object base, Object property) throws ELException {
   		if (base == null) {
   			String beanName = property.toString();
   			BeanFactory bf = getBeanFactory(elContext);
   			if (bf.containsBean(beanName)) {
   				return true;
   			}
   		}
   		return false;
   	}
   
   	@Override
  	public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext elContext, Object base) {
  		return null;
  	}
  
  	@Override
  	public Class<?> getCommonPropertyType(ELContext elContext, Object base) {
  		return Object.class;
  	}
  
  
  	/**
  	 * Retrieve the Spring BeanFactory to delegate bean name resolution to.
  	 * @param elContext the current ELContext
  	 * @return the Spring BeanFactory (never <code>null</code>)
  	 */
  	protected BeanFactory getBeanFactory(ELContext elContext) {
   		return getWebApplicationContext(elContext);
  	}
}
