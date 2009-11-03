package org.aphreet.c3.web.webbeans.document;

import org.aphreet.c3.web.entity.DocumentVersion;
import org.aphreet.c3.web.util.HttpUtil;
import org.aphreet.springframework.web.HttpParam;

public class DocumentViewBean extends AbstractDocumentBean{

	@HttpParam
	private Integer rev;
	
	private DocumentVersion version;
	
	private String revisionNumber = "";
	
	@Override
	protected void load() {
		
		if(!isMember){
			HttpUtil.sendAccessDenied();
			return;
		}
		
		if(rev != null && rev > 0 && rev <= document.getVersions().size()){
			version = document.getVersions().get(rev-1);
			revisionNumber = rev.toString();
		}else{
			version = document.getHeadVersion();
		}
	}

	public Integer getRev() {
		return rev;
	}

	public void setRev(Integer rev) {
		this.rev = rev;
	}

	public DocumentVersion getVersion() {
		return version;
	}

	public void setVersion(DocumentVersion version) {
		this.version = version;
	}

	public String getRevisionNumber() {
		return revisionNumber;
	}

}
