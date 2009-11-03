package org.aphreet.c3.web.entity;

import java.util.HashSet;
import java.util.Set;

public class WorkGroup extends AbstractGroup{

	private static final long serialVersionUID = -8297154332903524382L;

	private Set<User> members;
	
	public Set<User> getMembers() {
		return members;
	}

	public void setMembers(Set<User> members) {
		this.members = members;
	}

	@Override
	public boolean isMember(User user) {
		return (owner == user) || members.contains(user) ;
	}

	@Override
	public Set<User> getAllMembers() {
		HashSet<User> result = new HashSet<User>();
		result.add(owner);
		result.addAll(this.getMembers());
		return result;
	}
}
