package org.mygroup.vertxrs.template;

import java.util.HashMap;
import java.util.Map;

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
}
