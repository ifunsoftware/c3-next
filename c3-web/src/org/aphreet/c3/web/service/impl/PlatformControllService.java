package org.aphreet.c3.web.service.impl;

import org.aphreet.c3.platform.management.*;
import org.aphreet.c3.web.service.IPlatformControllService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlatformControllService implements IPlatformControllService{

	@Autowired
	private PlatformManagementEndpoint platformManagementEndpoint;
	
	public void setPlatformAccessEndpoint(
			PlatformManagementEndpoint platformManagementEndpoint) {
		this.platformManagementEndpoint = platformManagementEndpoint;
	}
	
	@Override
	public void createStorage(String type, String path) {
		platformManagementEndpoint.createStorage(type, path);
		
	}

}
