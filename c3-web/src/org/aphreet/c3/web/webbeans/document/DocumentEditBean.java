package org.aphreet.c3.web.webbeans.document;

import org.aphreet.c3.web.util.HttpUtil;
import org.aphreet.c3.web.util.PathBuilder;


public class DocumentEditBean extends AbstractDocumentBean{

	@Override
	protected void load() {
		if(!isMember){
			HttpUtil.sendAccessDenied();
			return;
		}
	}
	
	public String deleteDocument(){		
		resourceService.delete(document, requestBean.getCurrentUser());
		
		HttpUtil.sendRedirect(new PathBuilder("/group/documents.xhtml").addParam("id", group.getId()).toString());
		
		return "success";
	}

}
