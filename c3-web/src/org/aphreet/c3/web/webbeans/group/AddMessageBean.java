package org.aphreet.c3.web.webbeans.group;

import java.util.Date;

import org.aphreet.c3.web.entity.Message;
import org.aphreet.c3.web.service.IMessageService;
import org.aphreet.c3.web.util.HttpUtil;
import org.aphreet.c3.web.webbeans.RequestBean;
import org.hibernate.validator.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;

public class AddMessageBean extends IdGroupViewBean{

	@NotEmpty
	private String title;
	
	@NotEmpty
	private String text;
	
	@Autowired
	private RequestBean requestBean;
	
	@Autowired
	private IMessageService messageService;
	
	@Override
	protected void load() {
		if(!isMember){
			HttpUtil.sendAccessDenied();
		}
	}
	
	public String addMessage(){
		Message message = new Message();
		message.setTitle(title);
		message.setBody(text);
		message.setGroup(group);
		message.setCreateDate(new Date());
		message.setOwner(requestBean.getCurrentUser());
		
		messageService.addMessage(message);
		
		HttpUtil.sendRedirect("/group/messages.xhtml?id=" + groupId);
		
		return "success";
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

}
