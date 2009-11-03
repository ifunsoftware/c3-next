package org.aphreet.c3.web.webbeans;

import java.util.List;

import javax.annotation.PostConstruct;

import org.aphreet.c3.web.entity.WorkGroup;
import org.aphreet.c3.web.service.IGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("request")
public class MainPageBean {

	@Autowired
	private IGroupService groupService;
	
	private List<WorkGroup> groups;
	
	@PostConstruct
	public void load(){
		groups = groupService.getTopGroups();
	}

	public List<WorkGroup> getGroups() {
		return groups;
	}

	public void setGroups(List<WorkGroup> groups) {
		this.groups = groups;
	}
}
