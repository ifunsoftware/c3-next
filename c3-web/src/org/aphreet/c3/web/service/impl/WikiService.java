package org.aphreet.c3.web.service.impl;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.web.dao.ResourceDao;
import org.aphreet.c3.web.dao.WikiDao;
import org.aphreet.c3.web.entity.AbstractGroup;
import org.aphreet.c3.web.entity.Document;
import org.aphreet.c3.web.entity.Message;
import org.aphreet.c3.web.entity.Content;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.entity.UserProfile;
import org.aphreet.c3.web.entity.WikiPage;
import org.aphreet.c3.web.entity.WikiPageVersion;
import org.aphreet.c3.web.message.MailingTask;
import org.aphreet.c3.web.service.IMessageService;
import org.aphreet.c3.web.service.IWikiService;
import org.aphreet.c3.web.service.impl.wiki.C3HtmlVisitor;
import org.aphreet.c3.web.util.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import be.devijver.wikipedia.Parser;
import be.devijver.wikipedia.SmartLink;
import be.devijver.wikipedia.SmartLinkResolver;
import be.devijver.wikipedia.SmartLink.SmartLinkType;

/**
 * Service provides wiki functionality
 * @author Mikhail Malygin
 *
 */
@Service
public class WikiService implements IWikiService {

	private final Log logger = LogFactory.getLog(getClass());
	
	@Autowired
	private WikiDao wikiDao;
	
	@Autowired
	private ResourceDao resourceDao;
	
	@Autowired
	private MessageSource messageSource;
	
	@Autowired
	private IMessageService messageSerivce;
	
	/**
	 * Find page with specified name in group namespace
	 * @param group
	 * @param name
	 * @return
	 */
	public WikiPage getPage(AbstractGroup group, String name){
		return wikiDao.getPage(group, name);
	}
	
	/**
	 * Get content of page in revision
	 * @param page
	 * @param revision
	 * @return
	 */
	public WikiPageVersion getPageVersion(WikiPage page, Integer revision){
		if(revision == null){
			return null;
		}
		
		try{
			return page.getVersions().get(revision-1);
		}catch(IndexOutOfBoundsException e){
			return null;
		}
	}
	
	/**
	 * Updates wiki page
	 * Each update crates new page revision
	 * @param page
	 * @param text
	 * @param editor
	 */
	public void updatePage(WikiPage page, String text, User editor, boolean notify){
		if(!page.getHeadVersion().getBody().equals(text)){
			WikiPageVersion version = new WikiPageVersion();
			version.setBody(text);
			version.setHtmlBody(this.parseWikiText(text, page.getGroup()));
			version.setEditDate(new Date());
			version.setEditor(editor);
			page.addVersion(version);
			page.setLastEditDate(version.getEditDate());
			
			if(notify){
				processNotifiaction(page, false);
			}
		}
	}
	
	/**
	 * Craete new page
	 * @param page
	 * @param text
	 */
	public void createPage(WikiPage page, String text, boolean notify){
		WikiPageVersion version = new WikiPageVersion();
		version.setBody(text);
		version.setHtmlBody(this.parseWikiText(text, page.getGroup()));
		version.setEditDate(page.getCreateDate());
		version.setEditor(page.getOwner());
		
		page.addVersion(version);
		
		wikiDao.persist(page);
		
		if(notify){
			processNotifiaction(page, true);
		}
	}
	
	public String parseWikiText(String input, final AbstractGroup group){
		
		StringWriter writer = new StringWriter();
		
		new Parser().withVisitor(input.replaceAll("[^\r]\n", "\r\n"), new C3HtmlVisitor(writer, new SmartLinkResolver(){

			public SmartLink resolve(String arg0) {
				return WikiService.this.getResourceLink(arg0, group);
			}
			
		}));
		
		return writer.toString();
		
	}
	
	private SmartLink getResourceLink(String key, AbstractGroup group){
		
		try{
			if(key.startsWith("Resource:")){
				Integer id = Integer.parseInt(key.substring("Resource:".length()));
				Content res = resourceDao.getResourceById(id);
				if(res != null){
					if(res.getGroup() == group){
						
						String url = "";
						
						if(res instanceof Message){
							url = "message.xhtml?id=" + res.getId();
						}else if(res instanceof Document){
							url = "group/document.xhtml?id=" + res.getId();
						}else{
							url = "resource.xhtml?id=" + res.getId();
						}
						
						
						return new SmartLink(url, res.getTitle(), SmartLinkType.A_LINK);
					}
				}
			}else{
				if(key.startsWith("Image:")){
					Integer id = Integer.parseInt(key.substring("Image:".length()));
					Document doc = resourceDao.getDocumentById(id);
					if(doc != null){
						if(doc.getContentType().startsWith("image")){
							if(doc.getGroup() == group){
								return new SmartLink("download?id=" + doc.getResourceAddress(), 
										doc.getName(), 
										"resource.xhtml?id=" + doc.getId(),
										SmartLinkType.IMG_LINK);
							}
						}
					}
				}
			}
		}catch(Exception e){
			logger.info("SL resolve exception" + e.getClass().getCanonicalName());
		}
		
		return new SmartLink("group/wiki.xhtml?id=" + group.getId() + "&name=" + key, key, SmartLinkType.A_LINK);
	}

	@Override
	public void deletePage(WikiPage page) {
		wikiDao.delete(page);
	}
	
	private void processNotifiaction(WikiPage page, boolean isNew){
		AbstractGroup group = page.getGroup();
		Set<User> members = group.getAllMembers();
		
		Set<String> addresses = new HashSet<String>();
		
		for (User user : members) {
			if(user.getUserProfile() != null){
				if(user.getUserProfile().getBoolSetting(UserProfile.NOTIFY_PROP)){
					addresses.add(user.getMail());
				}
			}
			
		}
		
		String title = messageSource.getMessage("wiki_notification_title", new Object[]{page.getTitle()},"Default", SpringUtil.getCurrentLocale());
		
		String bodyMessage;
		
		if(isNew){
			bodyMessage = "wiki_notification_new";
		}else{
			bodyMessage = "wiki_notification_edit";
		}
		
		String encodedPageTitle = page.getTitle();
		
		try {
			encodedPageTitle = URLEncoder.encode(page.getTitle(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		String body = messageSource.getMessage(bodyMessage, new Object[]{page.getHeadVersion().getEditor().getName(), page.getTitle(), page.getGroup().getId(), encodedPageTitle},"Default", SpringUtil.getCurrentLocale());
		
		messageSerivce.sendMessage(new MailingTask(title, body, page.getGroup().getUrlName(), addresses));
	}
	
}
