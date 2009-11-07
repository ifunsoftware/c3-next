package org.aphreet.c3.web.entity;

public class DocumentVersion extends ContentVersion{
	
	private static final long serialVersionUID = -2900165782867097488L;
	
	private int id;
	
	private long size;
	
	private Document document;

	public String getHumanReadableSize(){
		if(size < 1024){
			return size + " B";
		}else if(size < 1024 * 1024){
			return String.format("%.1f KB", ((double) size) / 1024);
		}else if(size < 1024 * 1024 * 1024){
			return String.format("%.1f MB", ((double) size) / (1024l * 1024l));
		}else{
			return String.format("%.1f GB", ((double) size /(1024 * 1024 * 1024l)));
		}
	}
	
	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	
}
