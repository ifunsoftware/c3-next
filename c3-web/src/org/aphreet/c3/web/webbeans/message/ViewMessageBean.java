package org.aphreet.c3.web.webbeans.message;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.aphreet.c3.web.entity.Message;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.entity.WorkGroup;
import org.aphreet.c3.web.exception.NotFoundException;
import org.aphreet.c3.web.service.IMessageService;
import org.aphreet.c3.web.util.HttpUtil;
import org.aphreet.c3.web.webbeans.RequestBean;
import org.aphreet.c3.web.webbeans.group.AbstractGroupViewBean;
import org.aphreet.springframework.web.HttpParam;
import org.hibernate.validator.NotEmpty;
import org.hibernate.validator.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

public class ViewMessageBean extends AbstractGroupViewBean{

	@Autowired
	private IMessageService messageService;
	
	@Autowired
	private RequestBean requestBean;
	
	@HttpParam("id")
	private Integer messageId;

	private String title;
	
	//TODO add validation (replyId must be in the current discussion)
	@NotNull
	private Integer replyId;
	
	@NotEmpty
	private String replyBody;
	
	private Message rootMessage;
	
	private List<MessageWrapper> messages;
	
	protected WorkGroup loadGroup(){
		
		Message message = messageService
			.loadDiscussionById(messageId, requestBean.getCurrentUser());
		
		if(message != null){
			rootMessage = message;
			//FIXME unsafe typecast
			return (WorkGroup) message.getGroup();
		}
		throw new NotFoundException();
	}
	
	@Override
	protected void load(){
		if(!isMember){
			HttpUtil.sendAccessDenied();
			return;
		}			
		
		this.title = rootMessage.getTitle();
		this.messages = getMessageList(rootMessage);
		//we need to set this id manually, because we have only message id param in request
	}

	public String addReply(){
		Message message = messageService.getMessageById(replyId);
		Message reply = new Message();

		reply.setTitle("Re: " + rootMessage.getTitle());
		reply.setOwner(requestBean.getCurrentUser());
		reply.setParent(message);
		reply.setGroup(message.getGroup());
		reply.setBody(replyBody);
		reply.setCreateDate(new Date());
	
		messageService.addMessage(reply);
		
		message.getChildren().add(reply);
		
		//reloading message list
		Message root = messageService
			.loadDiscussionById(messageId, requestBean.getCurrentUser());
		this.messages = getMessageList(root);
		
		return "success";
	}
	
	/**
	 * Returns List of {@link MessageWrapper} object build on
	 * {@link Message} tree
	 * @param root of the discussion
	 * @return
	 */
	private List<MessageWrapper> getMessageList(Message root){
		List<MessageWrapper> result = new LinkedList<MessageWrapper>();
		putMessages(root, result, 0);
		return result;
	}
	
	private void putMessages(Message message, List<MessageWrapper> list, int offset){
		list.add(new MessageWrapper(message, offset));
		if(message.getChildren() != null){
			for(Message child : message.getChildren()){
				putMessages(child, list, offset+1);
			}
		}
	}
	
	public Integer getMessageId() {
		return messageId;
	}

	public void setMessageId(Integer messageId) {
		this.messageId = messageId;
	}
	
	public List<MessageWrapper> getMessages() {
		return messages;
	}

	public void setMessages(List<MessageWrapper> messages) {
		this.messages = messages;
	}

	public void setReplyId(Integer replyId) {
		this.replyId = replyId;
	}

	public Integer getReplyId() {
		return replyId;
	}

	public void setReplyBody(String replyBody) {
		this.replyBody = replyBody;
	}

	public String getReplyBody() {
		return replyBody;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Data transfer object incapsulates
	 * Message instance and offset in the discussion tree
	 * @author Aphreet
	 *
	 */
	public class MessageWrapper{
		
		private Message message;
		private	int offset;
		
		public MessageWrapper(Message msg, int offset){
			this.message = msg;
			if(offset > 9){
				offset = 9;
			}
			this.offset = offset;
		}
		
		public Integer getId(){
			return message.getId();
		}
		
		public Date getDate(){
			return message.getCreateDate();
		}
		
		public String getBody(){
			return message.getBody();
		}
		
		public String getTitle(){
			return message.getTitle();
		}
		
		public User getOwner(){
			return message.getOwner();
		}
		
		public String getBlockClass(){
			return "comment-block comment-block-" + offset;
		}
	}
}
