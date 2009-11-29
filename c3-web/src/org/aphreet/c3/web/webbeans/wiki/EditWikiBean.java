package org.aphreet.c3.web.webbeans.wiki;

import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.aphreet.c3.web.entity.WikiPage;
import org.aphreet.c3.web.service.IWikiService;
import org.aphreet.c3.web.util.HttpUtil;
import org.aphreet.c3.web.util.PathBuilder;
import org.aphreet.c3.web.webbeans.group.IdGroupViewBean;
import org.aphreet.springframework.web.HttpParam;
import org.springframework.beans.factory.annotation.Autowired;

public class EditWikiBean extends IdGroupViewBean{

	@HttpParam("name")
	private String pageName;

	@Autowired
	private IWikiService wikiService;
	
	private WikiPage wikiPage;
	
	private Boolean newPage = false;
	
	private Boolean pageSaved = false;

	private String text;
	
	private boolean minorEdit = false;
	
	private boolean showPreview = false;
	
	private String wikiPreview = "";
	
	@Override
	protected void load() {
		if(!isMember){
			HttpUtil.sendNotFound();
			return;
		}
		
		wikiPage = wikiService.getPage(group, pageName);
		newPage = wikiPage == null;
		if(wikiPage == null){
			newPage = true;
			text = "";
		}else{
			newPage = false;
			text = wikiPage.getHeadVersion().getBody();
		}
	}
	
	public String savePage(){
		if(wikiPage == null){
			wikiPage = new WikiPage();
			wikiPage.setTitle(pageName);
			wikiPage.setCreateDate(new Date());
			wikiPage.setOwner(requestBean.getCurrentUser());
			wikiPage.setTitle(pageName);
			wikiPage.setGroup(group);
			wikiService.createPage(wikiPage, text, !minorEdit);
		}else{
			wikiService.updatePage(wikiPage, text, requestBean.getCurrentUser(), !minorEdit);
		}
		
		HttpUtil.sendRedirect(new PathBuilder("/group/wiki.xhtml")
			.addParam("id", groupId)
			.addParam("name", pageName).toString());
		pageSaved = true;
		return "success";
	}
	
	public String previewPage(){
		showPreview = true;
		
		wikiPreview = wikiService.parseWikiText(text, group);
		
		return "success";
	}
	
	public String deletePage(){
		if(wikiPage != null){
			wikiService.deletePage(wikiPage);
		}
		HttpUtil.sendRedirect(new PathBuilder("/group/wiki.xhtml")
		.addParam("id", groupId)
		.addParam("name", "Main").toString());
		return "success";
	}
	
	public void validateWiki(FacesContext context, 
			UIComponent toValidate, Object value) {
		
		String text = (String) value;
		
		try{
			wikiService.parseWikiText(text, group);
		}catch(Throwable e){
			
			while(e.getCause() != null){
				e = e.getCause();
			}
			
			((UIInput)toValidate).setValid(false);

			FacesMessage message = new FacesMessage("Failed to parse wiki text: " + e.getMessage());
			message.setSeverity(FacesMessage.SEVERITY_ERROR);
			context.addMessage(toValidate.getClientId(context), message);
		}
	}

	public String getPageName() {
		return pageName;
	}

	public void setPageName(String pageName) {
		this.pageName = pageName;
	}

	public Boolean getNewPage() {
		return newPage;
	}

	public void setNewPage(Boolean newPage) {
		this.newPage = newPage;
	}

	public WikiPage getWikiPage() {
		return wikiPage;
	}

	public void setWikiPage(WikiPage wikiPage) {
		this.wikiPage = wikiPage;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Boolean getPageSaved() {
		return pageSaved;
	}

	public void setPageSaved(Boolean pageSaved) {
		this.pageSaved = pageSaved;
	}

	public boolean isMinorEdit() {
		return minorEdit;
	}

	public boolean getShowPreview() {
		return showPreview;
	}

	public String getWikiPreview(){
		return wikiPreview;
	}

	public void setMinorEdit(boolean minorEdit) {
		this.minorEdit = minorEdit;
	}

}
