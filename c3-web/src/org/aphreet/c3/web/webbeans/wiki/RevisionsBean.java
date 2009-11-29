package org.aphreet.c3.web.webbeans.wiki;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.entity.WikiPage;
import org.aphreet.c3.web.entity.WikiPageVersion;
import org.aphreet.c3.web.service.IWikiService;
import org.aphreet.c3.web.util.HttpUtil;
import org.aphreet.c3.web.webbeans.group.IdGroupViewBean;
import org.aphreet.springframework.web.HttpParam;
import org.springframework.beans.factory.annotation.Autowired;

public class RevisionsBean extends IdGroupViewBean{

	@HttpParam("name")
	private String pageName;
	
	@HttpParam("rev")
	private Integer revision;
	
	@Autowired
	private IWikiService wikiService;
	
	private WikiPage wikiPage;
	
	private List<WikiRevisionDto> revisions = null;
	
	@Override
	protected void load() {
		wikiPage = wikiService.getPage(group, pageName);
		if(wikiPage == null){
			HttpUtil.sendNotFound();
		}
		
		List<WikiPageVersion> versions = wikiPage.getVersions();
		
		revisions = new ArrayList<WikiRevisionDto>();
		
		for(int i=0; i< versions.size(); i++){
			WikiPageVersion version = versions.get(i);
			revisions.add(new WikiRevisionDto(i+1, version.getEditDate(), version.getEditor(), version.getComment()));
		}
	}

	public String moveToHead(){
		FacesMessage message = new FacesMessage("Revert complete");
		message.setSeverity(FacesMessage.SEVERITY_INFO);
		FacesContext.getCurrentInstance().addMessage("wiki_versions_table", message);
		
		wikiService.createHeadFromVersion(revision, wikiPage, requestBean.getCurrentUser());
		load();
		return "success";
	}
	
	
	public String getPageName() {
		return pageName;
	}


	public void setPageName(String pageName) {
		this.pageName = pageName;
	}


	public WikiPage getWikiPage() {
		return wikiPage;
	}


	public List<WikiRevisionDto> getRevisions() {
		return revisions;
	}

	
	public Integer getRevision() {
		return revision;
	}

	public void setRevision(Integer revision) {
		this.revision = revision;
	}


	public class WikiRevisionDto{
		
		private final Integer revision;
		private final Date date;
		private final User user;
		private final String comment;
		
		public WikiRevisionDto(int revision, Date date, User user, String comment){
			this.revision = revision;
			this.date = date;
			this.user = user;
			this.comment = comment;
		}

		public Integer getRevision() {
			return revision;
		}

		public Date getDate() {
			return date;
		}

		public User getUser() {
			return user;
		}
		
		public String getComment(){
			return comment;
		}
		
	}
}


