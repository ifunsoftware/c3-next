package org.aphreet.c3.web.service;

import java.util.List;

import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.Message;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.message.MailingTask;

/**
 * 
 * Service provides messaging functionality
 * 
 * @author Mikhail Malygin
 *
 */

public interface IMessageService {

	/**
	 * 
	 * @param id Id of root message in discussion
	 * @param currentUser {@link User} that tries to view discussion
	 * @return Message instance if discussion exists or null if no such discussion 
	 * or user has no permission to view it.
	 */
	public Message loadDiscussionById(Integer id, User currentUser);

	/**
	 * Get list of discussions for group
	 * @param group
	 * @return
	 */
	public List<Message> getDiscussionList(AbstractGroup group);

	public Message getMessageById(Integer id);

	/**
	 * Add new message
	 * @param message
	 */
	public void addMessage(Message message);
	
	public void sendMessage(MailingTask task);

}