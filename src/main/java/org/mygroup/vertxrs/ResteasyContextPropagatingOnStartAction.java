package org.mygroup.vertxrs;

import rx.Single;
import rx.Single.OnSubscribe;
import rx.functions.Func2;

public class ResteasyContextPropagatingOnStartAction implements Func2<Single, OnSubscribe, OnSubscribe> {

	@Override
	public OnSubscribe call(Single t1, OnSubscribe t2) {
		System.err.println("On start: "+t1);
		return t2;
	}

}
