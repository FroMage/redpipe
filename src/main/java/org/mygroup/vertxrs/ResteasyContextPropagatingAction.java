package org.mygroup.vertxrs;

import rx.functions.Action0;

public class ResteasyContextPropagatingAction implements Action0 {

	private Runnable runnable;

	public ResteasyContextPropagatingAction(Action0 action0) {
		runnable = new ResteasyRunnableContextCatcher(action0);
	}

	@Override
	public void call() {
		runnable.run();
	}

}
