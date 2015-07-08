package org.activiti.rest.controller.adapter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

public class ServletOutputStreamCopier extends ServletOutputStream {

	private OutputStream outputStream;
	private ByteArrayOutputStream copy;

	public ServletOutputStreamCopier(OutputStream outputStream) {
		this.outputStream = outputStream;
		this.copy = new ByteArrayOutputStream(1024);
	}

	@Override
	public void write(int b) throws IOException {
		outputStream.write(b);
		copy.write(b);
	}

	public byte[] getCopy() {
		return copy.toByteArray();
	}
	
	@Override
	public String toString() {
		return copy.toString();
	}
	
	@Override
	public boolean isReady() {
		return false;
	}

	@Override
	public void setWriteListener(WriteListener writeListener) {
	}

}