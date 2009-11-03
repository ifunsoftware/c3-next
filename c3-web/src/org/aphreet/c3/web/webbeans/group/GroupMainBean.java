package org.aphreet.c3.web.webbeans.group;

import java.util.ArrayList;
import java.util.List;

import org.aphreet.c3.web.entity.User;

public class GroupMainBean extends IdGroupViewBean{

	private List<User> members = new ArrayList<User>();
	
	@Override
	protected void load() {
		members.addAll(group.getAllMembers());
	}

	public List<User> getMembers() {
		return members;
	}

	public void setMembers(List<User> members) {
		this.members = members;
	}

}
