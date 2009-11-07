package org.aphreet.springframework.resource;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.springframework.context.support.ResourceBundleMessageSource;

public class XMLResourceBundleMessageSource extends ResourceBundleMessageSource{
	
	protected ResourceBundle doGetBundle(String basename, Locale locale) throws MissingResourceException {
		return ResourceBundle.getBundle(basename, locale, getBundleClassLoader(), new XMLResourceBundleControl());
	}
}
