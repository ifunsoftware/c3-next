package org.aphreet.springframework.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

public class XMLResourceBundle extends ResourceBundle{
	private Properties props;

	XMLResourceBundle(InputStream stream) throws IOException {
		props = new Properties();
		props.loadFromXML(stream);
	}

	protected Object handleGetObject(String key) {
		return props.getProperty(key);
	}

	public Enumeration<String> getKeys() {
		Set<String> handleKeys = props.stringPropertyNames();
		return Collections.enumeration(handleKeys);
	}
}
