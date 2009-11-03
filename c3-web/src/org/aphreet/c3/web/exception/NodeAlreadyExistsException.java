package org.aphreet.c3.web.exception;

public class NodeAlreadyExistsException extends FileSystemException{

	private static final long serialVersionUID = -4386476614528566481L;

	public NodeAlreadyExistsException() {
		super();
	}

	public NodeAlreadyExistsException(String message, Throwable cause) {
		super(message, cause);
	}

	public NodeAlreadyExistsException(String message) {
		super(message);
	}

	public NodeAlreadyExistsException(Throwable cause) {
		super(cause);
	}
}
