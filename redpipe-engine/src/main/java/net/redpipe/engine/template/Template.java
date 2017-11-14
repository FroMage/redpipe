package net.redpipe.engine.template;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import net.redpipe.engine.core.AppGlobals;

import rx.Single;

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
	
	public Single<Response> render() {
		TemplateRenderer renderer = AppGlobals.get().getTemplateRenderer(name);
		if(renderer == null)
			throw new RuntimeException("Failed to find template renderer for template "+name);
		return renderer.render(name, variables);
	}
}
