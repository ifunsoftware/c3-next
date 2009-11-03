package org.aphreet.c3.web.webbeans.group;

import org.aphreet.c3.web.entity.WorkGroup;
import org.aphreet.c3.web.exception.NotFoundException;
import org.aphreet.c3.web.service.IGroupService;
import org.aphreet.springframework.web.HttpParam;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class IdGroupViewBean extends AbstractGroupViewBean{

	@HttpParam({"file_upload_form:id", "id"})
	protected Integer groupId;

	@Autowired
	protected IGroupService groupService;

	@Override
	protected WorkGroup loadGroup() throws NotFoundException {
		
		WorkGroup group = groupService.getGroupById(groupId);
		
		if(group != null){
			return group;
		}
		
		throw new NotFoundException();
	}
	
	
	public Integer getGroupId() {
		return groupId;
	}

	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

}
