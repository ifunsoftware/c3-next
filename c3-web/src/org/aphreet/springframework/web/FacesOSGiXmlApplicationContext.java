package org.aphreet.springframework.web;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.springsource.server.web.dm.ServerOsgiBundleXmlWebApplicationContext;

public class FacesOSGiXmlApplicationContext extends ServerOsgiBundleXmlWebApplicationContext{

	@Override
	protected DefaultListableBeanFactory createBeanFactory() {
		return new FacesListableBeanFactory(getInternalParentBeanFactory());
	}
}
