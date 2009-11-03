package org.aphreet.c3.web.webbeans.document;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.aphreet.c3.web.entity.DocumentVersion;
import org.aphreet.c3.web.entity.User;
import org.aphreet.c3.web.util.HttpUtil;

public class DocumentHistoryBean extends AbstractDocumentBean{

	private List<DocumentVersionDTO> versions = new LinkedList<DocumentVersionDTO>();
	
	@Override
	protected void load() {
		
		if(!isMember){
			HttpUtil.sendAccessDenied();
			return;
		}
		
		List<DocumentVersion> versions = document.getVersions();
		
		for(int i=0; i<versions.size(); i++){
			this.versions.add(new DocumentVersionDTO(i+1, versions.get(i)));
		}
		
	}

	public class DocumentVersionDTO{
		
		private User editor;
		
		private Date date;
		
		private String size;
		
		private int number;
		
		
		public DocumentVersionDTO(int number, DocumentVersion version) {
			this.number = number;
			this.editor = version.getEditor();
			this.date = version.getEditDate();
			this.size = version.getHumanReadableSize();
		}


		public User getEditor() {
			return editor;
		}


		public Date getDate() {
			return date;
		}


		public String getSize() {
			return size;
		}


		public int getNumber() {
			return number;
		}
		
	}

	public List<DocumentVersionDTO> getVersions() {
		return versions;
	}

}
