package org.aphreet.c3.web.webbeans.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.aphreet.c3.web.service.IConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
public class ShowConfigBean {

	@Autowired
	private IConfigService configService;
	
	private List<ConfigValue> configValues = new ArrayList<ConfigValue>();
	
	@PostConstruct
	public void init(){
		Map<String, String> props = configService.getProperties();
		
		for(String key : props.keySet()){
			configValues.add(new ConfigValue(key, props.get(key)));
		}
	}
	
	public List<ConfigValue> getConfigValues() {
		return configValues;
	}

	public class ConfigValue{
		
		private final String key;
		
		private final String value;

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}

		public ConfigValue(String key, String value) {
			super();
			this.key = key;
			this.value = value;
		}
	}
}
