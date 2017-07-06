package org.mygroup.vertxrs;

import java.util.Map;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import rx.functions.Action0;

public class ResteasyRunnableContextCatcher implements Runnable {

	private Map<Class<?>, Object> contextDataMap;
	private Action0 action;

	public ResteasyRunnableContextCatcher(Action0 action0) {
		contextDataMap = ResteasyProviderFactory.getContextDataMap();
		this.action = action0;
	}

	@Override
	public void run() {
		ResteasyProviderFactory.pushContextDataMap(contextDataMap);
		try{
			System.err.println("Restoring context data");
			action.call();
		}finally{
			System.err.println("Removing context data");
			ResteasyProviderFactory.removeContextDataLevel();
		}
	}

}
