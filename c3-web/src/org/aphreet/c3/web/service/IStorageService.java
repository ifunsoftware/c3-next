package org.aphreet.c3.web.service;

import java.io.OutputStream;

import org.aphreet.c3.web.storage.ContentRevision;
import org.aphreet.c3.web.storage.ContentWrapper;
import org.aphreet.c3.web.storage.Principal;

public interface IStorageService {

	public ContentRevision addContent(ContentWrapper content, Principal principal);
	
	public ContentRevision updateContent(String ca, ContentWrapper content, Principal principal);
	
	public void getContent(String ca, OutputStream stream);
	
	public void getContent(String ca, long revision, OutputStream stream);
	
}

