package org.aphreet.c3.web.webdav.exception;

public class DavException extends RuntimeException{

	private static final long serialVersionUID = 7406447986801987189L;

	public DavException(){
		super();
	}
	
	public DavException(String msg){
		super(msg);
	}

	public DavException(String message, Throwable cause) {
		super(message, cause);
	}

	public DavException(Throwable cause) {
		super(cause);
	}
	
}
