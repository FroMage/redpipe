package net.redpipe.engine.template;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.reactivex.Single;
import net.redpipe.engine.core.AppGlobals;

public class Template {
	private Map<String, Object> variables;
	private String name;

	public Template(String name, Map<String, Object> variables){
		this.name = name;
		this.variables = variables;
	}

	public Template(String name){
		this(name, new HashMap<>());
	}

	public Map<String, Object> getVariables() {
		return variables;
	}
	
	public String getName() {
		return name;
	}
	
	public Template set(String name, Object value){
		variables.put(name, value);
		return this;
	}
	
	public Single<Response> render(String variant) {
		TemplateRenderer renderer = AppGlobals.get().getTemplateRenderer(name);
		if(renderer == null)
			throw new RuntimeException("Failed to find template renderer for template "+name);
		return renderer.render(name, variables, variant);
	}
	
	public static MediaType parseMediaType(String extension) {
		// FIXME: bigger list, and override in config
		if(extension.equalsIgnoreCase("html"))
			return MediaType.TEXT_HTML_TYPE;
		if(extension.equalsIgnoreCase("xml"))
			return MediaType.APPLICATION_XML_TYPE;
		if(extension.equalsIgnoreCase("txt"))
			return MediaType.TEXT_PLAIN_TYPE;
		if(extension.equalsIgnoreCase("json"))
			return MediaType.APPLICATION_JSON_TYPE;
		System.err.println("Unknown extension type: "+extension);
		return MediaType.APPLICATION_OCTET_STREAM_TYPE;
	}

	public static MediaType parseMediaType(String templatePath, String templateExtension) {
		int lastSlash = templatePath.lastIndexOf('/');
		String templateName;
		if(lastSlash != -1)
			templateName = templatePath.substring(lastSlash+1);
		else
			templateName = templatePath;
		if(templateName.endsWith(templateExtension))
			templateName = templateName.substring(0, templateName.length()-templateExtension.length());
		int lastDot = templateName.lastIndexOf('.');
		if(lastDot != -1)
			return parseMediaType(templateName.substring(lastDot+1));
		// no extension
		return MediaType.APPLICATION_OCTET_STREAM_TYPE;
	}
}
