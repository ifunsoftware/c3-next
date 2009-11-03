package org.aphreet.c3.web.exception;

public class AccessDeniedException extends RuntimeException{

	public AccessDeniedException() {
		super();
	}

	public AccessDeniedException(String message, Throwable cause) {
		super(message, cause);
	}

	public AccessDeniedException(String message) {
		super(message);
	}

	public AccessDeniedException(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = 2930573249519512107L;

}
