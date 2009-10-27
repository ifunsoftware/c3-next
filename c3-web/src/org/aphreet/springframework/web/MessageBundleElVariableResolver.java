package org.aphreet.springframework.web;

import java.util.Locale;

import javax.el.ELContext;
import javax.el.ELException;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.el.SpringBeanFacesELResolver;

public class MessageBundleElVariableResolver extends SpringBeanFacesELResolver {

	@Override
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

}
