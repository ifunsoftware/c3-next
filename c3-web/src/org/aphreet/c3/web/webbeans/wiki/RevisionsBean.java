package org.aphreet.c3.web.webbeans.wiki;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
	
	@Autowired
	private IWikiService wikiService;
	
	private WikiPage wikiPage;
	
	private List<WikiRevisionDto> revisions = new ArrayList<WikiRevisionDto>();
	
	@Override
	protected void load() {
		wikiPage = wikiService.getPage(group, pageName);
		if(wikiPage == null){
			HttpUtil.sendNotFound();
		}
		
		List<WikiPageVersion> versions = wikiPage.getVersions();
		
		for(int i=0; i< versions.size(); i++){
			revisions.add(new WikiRevisionDto(i+1, versions.get(i).getEditDate(), versions.get(i).getEditor()));
		}
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

	
	public class WikiRevisionDto{
		
		private final Integer revision;
		private final Date date;
		private final User user;
		
		public WikiRevisionDto(int revision, Date date, User user){
			this.revision = revision;
			this.date = date;
			this.user = user;
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
		
	}
}


