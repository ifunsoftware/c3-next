package org.aphreet.c3.web.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

import org.aphreet.c3.web.util.collection.CollectionUtil;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;


public abstract class ContentWrapper {

	public static ContentWrapper wrap(File file){
		return new FileContentWrapper(file);
	}
	
	public static ContentWrapper wrap(byte[] data){
		return new ByteContentWrapper(data);
	}
	
	public abstract MimeType getMimeType();
	
	public abstract long length();
	
	public abstract void writeTo(WritableByteChannel ch) throws IOException;
	
	public abstract InputStream getInputStream() throws IOException;
}

class FileContentWrapper extends ContentWrapper{

	private final File file;
	
	public FileContentWrapper(File file){
		if(file == null){
			throw new NullPointerException();
		}
		this.file = file;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public MimeType getMimeType() {
		return (MimeType) CollectionUtil.top(MimeUtil.getMimeTypes(file));
	}

	@Override
	public long length() {
		return file.length();
	}

	@Override
	public void writeTo(WritableByteChannel ch) throws IOException{
		FileChannel channel = new FileInputStream(file).getChannel();
		try{
			channel.transferTo(0, file.length(), ch);
		}finally{
			if(channel != null){
				channel.close();
			}
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new FileInputStream(file);
	}
	
}

class ByteContentWrapper extends ContentWrapper{

	private final byte[] data;
	
	public ByteContentWrapper(byte[] data) {
		if(data == null){
			throw new NullPointerException();
		}
		this.data = data;
	}

	@SuppressWarnings("unchecked")
	@Override
	public MimeType getMimeType() {
		return (MimeType) CollectionUtil.top(MimeUtil.getMimeTypes(data));
	}

	@Override
	public long length() {
		return data.length;
	}

	@Override
	public void writeTo(WritableByteChannel channel) throws IOException {
		channel.write(ByteBuffer.wrap(data));
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(data);
	}	
}


