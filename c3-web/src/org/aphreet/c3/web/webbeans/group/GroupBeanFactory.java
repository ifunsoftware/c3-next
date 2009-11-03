package org.aphreet.c3.web.webbeans.group;

import java.util.HashMap;
import java.util.Map;

import org.aphreet.c3.web.util.FacesUtil;
import org.aphreet.c3.web.webbeans.document.DocumentEditBean;
import org.aphreet.c3.web.webbeans.document.DocumentHistoryBean;
import org.aphreet.c3.web.webbeans.document.DocumentViewBean;
import org.aphreet.c3.web.webbeans.message.ViewMessageBean;
import org.aphreet.c3.web.webbeans.wiki.EditWikiBean;
import org.aphreet.c3.web.webbeans.wiki.RevisionsBean;
import org.aphreet.c3.web.webbeans.wiki.ViewWikiBean;

public class GroupBeanFactory {

	private Map<String, Class<? extends AbstractGroupViewBean>> beans = new HashMap<String, Class<? extends AbstractGroupViewBean>>();
	
	public GroupBeanFactory(){
		beans.put("/group/wiki.jspx", ViewWikiBean.class);
		beans.put("/group/wiki/edit.jspx", EditWikiBean.class);
		beans.put("/group/wiki/history.jspx", RevisionsBean.class);
		
		beans.put("/message.jspx", ViewMessageBean.class);
		beans.put("/group/messages.jspx", GroupMessageBean.class);
		beans.put("/group/addmsg.jspx", AddMessageBean.class);
		
		beans.put("/group/documents.jspx", GroupDocumentsBean.class);
		beans.put("/group/main.jspx", GroupMainBean.class);
		beans.put("/group/manage.jspx", GroupManageBean.class);
		
		beans.put("/group/document.jspx", DocumentViewBean.class);
		beans.put("/group/document/edit.jspx", DocumentEditBean.class);
		beans.put("/group/document/history.jspx", DocumentHistoryBean.class);
	}
	
	public AbstractGroupViewBean createGroupBean(){
		String viewId = FacesUtil.getViewRootId();
		
		Class<? extends AbstractGroupViewBean> beanClass = beans.get(viewId);
		
		if(beanClass != null){
			try {
				return beanClass.newInstance();
			} catch (Exception e) {
				throw new IllegalArgumentException("Can't create group bean for view: " + viewId, e);
			}
		}else{
			throw new IllegalArgumentException("Can't find group bean for view: " + viewId);
		}
		
	}
	
}
