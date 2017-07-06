package org.mygroup.vertxrs;

import rx.functions.Action0;
import rx.functions.Func1;

public final class ResteasyContextPropagatingOnScheduleAction implements Func1<Action0, Action0> {
	@Override
	public Action0 call(final Action0 action0) {
		System.err.println("schedule action?");
		return new ResteasyContextPropagatingAction(action0);
	}
}