package org.aphreet.c3.web.webbeans.wiki;

import org.aphreet.c3.web.entity.WikiPage;
import org.aphreet.c3.web.entity.WikiPageVersion;
import org.aphreet.c3.web.service.IWikiService;
import org.aphreet.c3.web.webbeans.group.IdGroupViewBean;
import org.aphreet.springframework.web.HttpParam;
import org.springframework.beans.factory.annotation.Autowired;

public class ViewWikiBean extends IdGroupViewBean{

	@HttpParam("name")
	private String pageName;
	
	@HttpParam("rev")
	private Integer revision;

	@Autowired
	private IWikiService wikiService;
	
	private WikiPage wikiPage;
	
	private String text;
	
	private Boolean newPage = true;
	
	private Boolean showRevision = false;

	@Override
	protected void load() {
		wikiPage = wikiService.getPage(group, pageName);
		if(wikiPage != null){
			newPage = false;
			WikiPageVersion version = wikiService.getPageVersion(wikiPage, revision);
			
			if(version != null){
				text = version.getHtmlBody();
				
				if(wikiPage.getHeadVersion() != version){
					showRevision = true;
				}
				
			}else{
				text = wikiPage.getHeadVersion().getHtmlBody();
			}
			
			
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

	public Integer getRevision() {
		return revision;
	}

	public void setRevision(Integer revision) {
		this.revision = revision;
	}

	public Boolean getShowRevision() {
		return showRevision;
	}

	public void setShowRevision(Boolean showRevision) {
		this.showRevision = showRevision;
	}

}
