package org.aphreet.c3.web.service.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.web.dao.ResourceDao;
import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.Message;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.entity.UserProfile;
import org.aphreet.c3.web.message.MailingTask;
import org.aphreet.c3.web.service.IConfigService;
import org.aphreet.c3.web.service.IMessageService;
import org.aphreet.c3.web.service.IWikiService;
import org.aphreet.c3.web.util.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
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
	
	@Autowired
	private IConfigService configService;
	
	@Autowired
	private JavaMailSender mailSender;
	
	private final Log logger = LogFactory.getLog(getClass());
	

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
		if(!configService.isMailEnabled()){
			return;
		}
		List<InternetAddress> recipients = new LinkedList<InternetAddress>();
		
		for(String address:task.getAddresses()){
			try{
				recipients.add(new InternetAddress(address));
			}catch(AddressException e){
				logger.warn("wrong address: " + address);
			}
		}
		
		if(recipients.isEmpty()){
			return;
		}
		
		try{
			InternetAddress groupAddress = new InternetAddress(task.getGroup() + "@" + configService.getMailDomain());
			
			MimeMessage msg = mailSender.createMimeMessage();
			msg.setFrom(groupAddress);
			msg.setRecipient(javax.mail.Message.RecipientType.TO, groupAddress);
			msg.setReplyTo(new InternetAddress[]{groupAddress});
			msg.setRecipients(javax.mail.Message.RecipientType.BCC, recipients.toArray(new InternetAddress[]{}));
			
			msg.setSubject(task.getTitle());
			msg.setText(task.getMessage());
			msg.setHeader("X-Mailer", "JavaMailer");
			msg.setSentDate(new Date());
			
			mailSender.send(msg);
			
		}catch(Exception e){
			logger.warn(e.getClass().getCanonicalName() + " " + e.getMessage());
		}
	}
}
