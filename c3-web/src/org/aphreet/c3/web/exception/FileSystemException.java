package org.aphreet.c3.web.exception;

public class FileSystemException extends RuntimeException{

	private static final long serialVersionUID = 4810020705047770158L;

	public FileSystemException() {
		super();
	}

	public FileSystemException(String message, Throwable cause) {
		super(message, cause);
	}

	public FileSystemException(String message) {
		super(message);
	}

	public FileSystemException(Throwable cause) {
		super(cause);
	}

}
