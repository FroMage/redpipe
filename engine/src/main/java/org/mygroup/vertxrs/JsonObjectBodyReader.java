package org.mygroup.vertxrs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;

import io.vertx.core.json.JsonObject;

@Provider
public class JsonObjectBodyReader implements MessageBodyReader<JsonObject>{

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return type == JsonObject.class;
	}

	@Override
	public JsonObject readFrom(Class<JsonObject> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		byte[] byteArray = IOUtils.toByteArray(entityStream);
		// FIXME: encoding
		return new JsonObject(new String(byteArray, "utf-8"));
	}


}
