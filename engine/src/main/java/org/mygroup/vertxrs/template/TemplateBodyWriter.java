package org.mygroup.vertxrs.template;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.vertx.core.buffer.Buffer;

@Provider
public class TemplateBodyWriter implements MessageBodyWriter<Template>{

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return type == Template.class;
	}

	public Buffer render(String template, Map<String,Object> variables){
		Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
		try{
			configuration.setTemplateLoader(new FileTemplateLoader(new File("src/main/resources")));
			freemarker.template.Template templ = configuration.getTemplate(template);
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				// FIXME: workaround to follow wiki
				Map<String, Object> variables2 = new HashMap<>(1);
				variables2.put("context", variables);
				templ.process(variables2, new OutputStreamWriter(baos));
				return Buffer.buffer(baos.toByteArray());
			}
		}catch(TemplateException | IOException x){
			throw new RuntimeException(x);
		}
	}

	@Override
	public long getSize(Template t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return render(t.getName(), t.getVariables()).length();
	}

	@Override
	public void writeTo(Template t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
			throws IOException, WebApplicationException {
		// TODO: encoding
		String txt = render(t.getName(), t.getVariables()).toString();
		entityStream.write(txt.getBytes("utf-8"));
	}

}
