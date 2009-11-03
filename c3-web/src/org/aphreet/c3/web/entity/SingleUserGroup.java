package org.aphreet.c3.web.entity;

import java.util.Collections;
import java.util.Set;

public class SingleUserGroup extends AbstractGroup{

	private static final long serialVersionUID = 1906486219647569246L;

	@Override
	public boolean isMember(User user) {
		return user == owner;
	}

	@Override
	public Set<User> getAllMembers() {
		return Collections.singleton(owner);
	}

}
