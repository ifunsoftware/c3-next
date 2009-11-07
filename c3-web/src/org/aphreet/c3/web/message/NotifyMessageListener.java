package org.aphreet.c3.web.message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component
public class NotifyMessageListener{

	
	@SuppressWarnings("unused")
	private final static Log log = LogFactory.getLog(NotifyMessageListener.class);
	
//		List<InternetAddress> recipients = new LinkedList<InternetAddress>();
//		
//		for(String address:task.getAddresses()){
//			try{
//				recipients.add(new InternetAddress(address));
//			}catch(AddressException e){
//				logger.warn("wrong address: " + address);
//			}
//		}
//		
//		try{
//			InternetAddress groupAddress = new InternetAddress(task.getGroup() + "@" + settingsBean.get("mailDomain"));
//			
//			MimeMessage msg = new MimeMessage(mailSession);
//			msg.setFrom();
//			msg.setRecipient(javax.mail.Message.RecipientType.TO, groupAddress);
//			msg.setReplyTo(new InternetAddress[]{groupAddress});
//			msg.setRecipients(javax.mail.Message.RecipientType.BCC, recipients.toArray(new InternetAddress[]{}));
//			
//			msg.setSubject(task.getTitle());
//			msg.setText(task.getMessage());
//			msg.setHeader("X-Mailer", "JavaMailer");
//			msg.setSentDate(new Date());
//			Transport.send(msg);
//		}catch(Exception e){
//			logger.warn(e.getClass().getCanonicalName() + " " + e.getMessage());
//		}
}

