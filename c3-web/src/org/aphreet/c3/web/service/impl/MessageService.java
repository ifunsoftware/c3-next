package org.aphreet.c3.web.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aphreet.c3.web.dao.ResourceDao;
import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.Message;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.entity.UserProfile;
import org.aphreet.c3.web.message.MailingTask;
import org.aphreet.c3.web.service.IMessageService;
import org.aphreet.c3.web.service.IWikiService;
import org.aphreet.c3.web.util.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/**
 * 
 * Service provides messaging functionality
 * 
 * @author Mikhail Malygin
 *
 */		

@Service
public class MessageService implements IMessageService {

	@Autowired
	private ResourceDao resourceDao;
	
	@Autowired
	private IWikiService wikiService;

	@Autowired
	private MessageSource messageSource;
	

	public Message loadDiscussionById(Integer id, User currentUser){
		Message message = resourceDao.getMessageById(id);
		
		if(message != null && message.getParent() == null){
			return message;
		}
		return null;
	}
	
	public List<Message> getDiscussionList(AbstractGroup group){
		return resourceDao.getDiscussionList(group);
	}
	
	public Message getMessageById(Integer id){
		return resourceDao.getMessageById(id);
	}

	public void addMessage(Message message){
		String input = message.getBody();
		String text = wikiService.parseWikiText(input, message.getGroup());
		message.setBody(text);
		resourceDao.persist(message);
		this.processNotifiaction(message, input);
	}
	
	private void processNotifiaction(Message message, String text){
		AbstractGroup group = message.getGroup();
		Set<User> members = group.getAllMembers();
		
		Set<String> addresses = new HashSet<String>();
		
		for (User user : members) {
			if(user.getUserProfile() != null){
				if(user.getUserProfile().getBoolSetting(UserProfile.NOTIFY_PROP)){
					addresses.add(user.getMail());
				}
			}
			
		}
		
		String title = message.getTitle();
		
		String body = messageSource.getMessage("notify_message_body", new Object[]{message.getOwner().getName(), text},"Default", SpringUtil.getCurrentLocale());
		
		this.sendMessage(new MailingTask(title, body, message.getGroup().getUrlName(), addresses));
	}
	
	/**
	 * Send message to mailing queue
	 * @param messageContents
	 */
	public void sendMessage(MailingTask task){
		//TODO implement mail send
//		try {
//			Session session = connectionFactory.createConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
//			MessageProducer producer = session.createProducer(mailQueue);
//			
//			ObjectMessage message = session.createObjectMessage(task);
//			
//			producer.send(message);
//		} catch (JMSException e) {
//			e.printStackTrace();
//		}
	}
}
