package org.aphreet.c3.web.exception;

public class StorageException extends RuntimeException{

	private static final long serialVersionUID = -6859298488394069956L;

	public StorageException(){
		super();
	}
	
	public StorageException(String msg){
		super(msg);
	}
	
	public StorageException(Throwable t){
		super(t);
	}
	
	public StorageException(String msg, Throwable t){
		super(msg, t);
	}
}
