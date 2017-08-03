package org.mygroup.vertxrs;

import javax.ws.rs.container.ContainerResponseContext;

public interface Session {

	public void save(ContainerResponseContext responseContext);

	public void put(String key, String value);

	public String get(String key);
	
	public void remove(String key);

	public void clear();
}
