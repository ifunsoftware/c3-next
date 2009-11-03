package org.aphreet.c3.web.webbeans.group;

import java.util.List;

import org.aphreet.c3.web.entity.Message;
import org.aphreet.c3.web.service.IMessageService;
import org.aphreet.c3.web.util.HttpUtil;
import org.springframework.beans.factory.annotation.Autowired;

public class GroupMessageBean extends IdGroupViewBean{

	private List<Message> messages;
	
	@Autowired
	private IMessageService messageService;
	
	@Override
	protected void load() {
		if(!isMember){
			HttpUtil.sendAccessDenied();
			return;
		}
		messages = messageService.getDiscussionList(group);
	}

	public List<Message> getMessages() {
		return messages;
	}

	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

}
