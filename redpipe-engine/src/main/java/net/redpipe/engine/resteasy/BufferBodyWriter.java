package net.redpipe.engine.resteasy;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import io.vertx.rxjava.core.buffer.Buffer;

@Provider
public class BufferBodyWriter implements MessageBodyWriter<Buffer>{

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return type == Buffer.class;
	}

	@Override
	public void writeTo(Buffer buffer, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
			throws IOException, WebApplicationException {
		for(int i=0;i<buffer.length();i++)
			entityStream.write(buffer.getByte(i));
	}

}
