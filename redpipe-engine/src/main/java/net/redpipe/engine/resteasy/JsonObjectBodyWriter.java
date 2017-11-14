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

import io.vertx.core.json.JsonObject;

@Provider
public class JsonObjectBodyWriter implements MessageBodyWriter<JsonObject>{

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return type == JsonObject.class;
	}

	@Override
	public long getSize(JsonObject t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return t.encode().length();
	}

	@Override
	public void writeTo(JsonObject t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
			throws IOException, WebApplicationException {
		// TODO: encoding
		entityStream.write(t.encode().getBytes("utf-8"));
	}

}
