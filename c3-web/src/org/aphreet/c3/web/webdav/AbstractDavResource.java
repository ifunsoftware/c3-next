package org.aphreet.c3.web.webdav;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.util.SpringUtil;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Request.Method;

public abstract class AbstractDavResource implements Resource, PropFindableResource{

	protected DavBean davBean = (DavBean) SpringUtil.getBean("davBean");

	protected final Log log = LogFactory.getLog(getClass());
	
	protected User currentUser;
	
	@Override
	public Object authenticate(String user, String password) {
		currentUser = davBean.authenticate(user, password);
		if(currentUser != null){
			return new Auth(currentUser.getName());
		}else{
			return null;
		}
	}

	@Override
	public boolean authorise(Request request, Method method, Auth auth) {
		
		if(auth == null){
			return false;
		}else{
			if(currentUser != null){
				return true;
			}
			return false;
		}
	}

	@Override
	public String checkRedirect(Request arg0) {
		return null;
	}

	@Override
	public String getRealm() {
		return "C3";
	}

	@Override
	public Date getModifiedDate() {
		return new Date();
	}

	@Override
	public String getUniqueId() {
		return null;
	}
	
}
