package org.aphreet.c3.web.webbeans.group;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.entity.WorkGroup;
import org.aphreet.c3.web.exception.NotFoundException;
import org.aphreet.c3.web.service.IGroupService;
import org.aphreet.c3.web.util.FacesUtil;
import org.aphreet.c3.web.util.HttpUtil;
import org.aphreet.c3.web.webbeans.RequestBean;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractGroupViewBean {

	protected Log logger = LogFactory.getLog(getClass());
	
	@Autowired
	protected IGroupService groupService;
	
	@Autowired
	protected RequestBean requestBean;
	
	protected WorkGroup group;
	
	protected Boolean isManager = false;
	
	protected Boolean isMember = false;
	
	private String viewId;
	
	@PostConstruct
	public void loadData(){
		try{
			User currentUser = requestBean.getCurrentUser();
			group = this.loadGroup();
			isManager = (group.getOwner() ==  currentUser);
			isMember = group.isMember(currentUser);

			viewId = FacesUtil.getViewRootId();

			this.load();
		}catch(NotFoundException e){
			HttpUtil.sendNotFound();
		}
	}
	
	protected abstract WorkGroup loadGroup() throws NotFoundException;
		
	protected abstract void load();
	

	public String getMsgStyle(){
		
		return getStyle("/message.jspx", "/group/messages.jspx", "/group/addmsg.jspx");
	}
	
	public String getDocStyle(){
		return getStyle("/group/documents.jspx", "/group/document.jspx", "/group/document/edit.jspx", "/group/document/history.jspx");
	}
	
	public String getWikiStyle(){
		return getStyle("/group/wiki.jspx", "/group/wiki/edit.jspx", "/group/wiki/history.jspx", "/group/wiki/diff.jspx");
	}
	
	public String getMainStyle(){
		return getStyle("/group/main.jspx");
	}
	
	public String getMngStyle(){
		return getStyle("/group/manage.jspx");
	}
	
	private String getStyle(String ... views){
		for (String string : views) {
			if(string.equals(viewId)){
				return "head_menu_selected";
			}
		}
		return "";
	}
	
	
	
	public WorkGroup getGroup() {
		return group;
	}

	public void setGroup(WorkGroup group) {
		this.group = group;
	}

	public Integer getGroupId() {
		return group.getId();
	}

	public Boolean getIsManager() {
		return isManager;
	}

	public void setIsManager(Boolean isManager) {
		this.isManager = isManager;
	}

}
