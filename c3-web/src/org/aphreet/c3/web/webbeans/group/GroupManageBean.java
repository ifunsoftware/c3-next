package org.aphreet.c3.web.webbeans.group;

import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.service.IGroupService;
import org.aphreet.c3.web.util.HttpUtil;
import org.aphreet.springframework.web.HttpParam;
import org.springframework.beans.factory.annotation.Autowired;

public class GroupManageBean extends IdGroupViewBean {

	@Autowired
	private IGroupService groupService;

	@HttpParam("deleteId")
	private Integer memberToDelete;
	
	private List<User> groupMembers;

	private String newMemeberName;

	@Override
	protected void load() {
		if (!isManager) {
			HttpUtil.sendAccessDenied();
			return;
		}
		groupMembers = new ArrayList<User>(group.getMembers());
	}

	public String addMember() {

		groupService.addGroupMember(group, newMemeberName);
		groupMembers = new ArrayList<User>(group.getMembers());
		newMemeberName = "";

		return "success";
	}

	public void delMember() {
		groupService.deleteGroupMember(group, memberToDelete);
		groupMembers = new ArrayList<User>(group.getMembers());
	}

	public void validateGroupMember(FacesContext context,
			UIComponent toValidate, Object value) {

		String name = (String) value;

		if (groupService.isUserAGroupMemeber(group, name)) {
			((UIInput) toValidate).setValid(false);
			FacesMessage message = new FacesMessage(
					"User is group member or owner");
			context.addMessage(toValidate.getClientId(context), message);
		}
	}

	public List<User> getGroupMembers() {
		return groupMembers;
	}

	public void setGroupMembers(List<User> groupMembers) {
		this.groupMembers = groupMembers;
	}

	public String getNewMemeberName() {
		return newMemeberName;
	}

	public void setNewMemeberName(String newMemeberName) {
		this.newMemeberName = newMemeberName;
	}

	public Integer getMemberToDelete() {
		return memberToDelete;
	}

	public void setMemberToDelete(Integer memberToDelete) {
		this.memberToDelete = memberToDelete;
	}

}
