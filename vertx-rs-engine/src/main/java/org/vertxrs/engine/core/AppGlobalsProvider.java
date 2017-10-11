package org.vertxrs.engine.core;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class AppGlobalsProvider implements ContextResolver<AppGlobals> {

	@Override
	public AppGlobals getContext(Class<?> type) {
		System.err.println("Providing app globals");
		return AppGlobals.get();
	}

}
