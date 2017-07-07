package org.mygroup.vertxrs;

import java.util.Map;

public class Template {
	private Map<String, Object> variables;
	private String name;

	public Template(String name, Map<String, Object> variables){
		this.name = name;
		this.variables = variables;
		
	}

	public Map<String, Object> getVariables() {
		return variables;
	}
	
	public String getName() {
		return name;
	}
}
