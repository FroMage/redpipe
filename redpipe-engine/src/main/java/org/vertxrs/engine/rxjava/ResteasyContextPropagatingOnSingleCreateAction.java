package org.vertxrs.engine.rxjava;

import java.util.Map;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.vertxrs.engine.core.AppGlobals;

import rx.Single;
import rx.Single.OnSubscribe;
import rx.SingleSubscriber;
import rx.functions.Func1;

public class ResteasyContextPropagatingOnSingleCreateAction implements Func1<OnSubscribe, OnSubscribe> {

	@Override
	public OnSubscribe call(OnSubscribe t) {
		return new ContextCapturerSingle(t);
	}
	
	final static class ContextCapturerSingle<T> implements Single.OnSubscribe<T> {

		final Map<Class<?>, Object> contextDataMap;

	    final Single.OnSubscribe<T> source;

		private AppGlobals appGlobals;

	    public ContextCapturerSingle(Single.OnSubscribe<T> source) {
	        this.source = source;
	        contextDataMap = ResteasyProviderFactory.getContextDataMap();
	        appGlobals = AppGlobals.get();
	    }

	    @Override
	    public void call(SingleSubscriber<? super T> t) {
	        source.call(new OnAssemblySingleSubscriber<T>(t, contextDataMap, appGlobals));
	    }

	    static final class OnAssemblySingleSubscriber<T> extends SingleSubscriber<T> {

	        final SingleSubscriber<? super T> actual;

	        final Map<Class<?>, Object> contextDataMap;

			private AppGlobals appGlobals;

	        public OnAssemblySingleSubscriber(SingleSubscriber<? super T> actual, Map<Class<?>, Object> contextDataMap, AppGlobals appGlobals) {
	            this.actual = actual;
	            this.contextDataMap = contextDataMap;
	            this.appGlobals = appGlobals;
	            actual.add(this);
	        }

	        @Override
	        public void onError(Throwable e) {
				ResteasyProviderFactory.pushContextDataMap(contextDataMap);
				AppGlobals previous = AppGlobals.set(appGlobals);
				try {
					actual.onError(e);
				}finally {
					AppGlobals.set(previous);
					ResteasyProviderFactory.removeContextDataLevel();
				}
	        }

	        @Override
	        public void onSuccess(T t) {
				ResteasyProviderFactory.pushContextDataMap(contextDataMap);
				AppGlobals previous = AppGlobals.set(appGlobals);
				try {
					actual.onSuccess(t);
				}finally {
					AppGlobals.set(previous);
					ResteasyProviderFactory.removeContextDataLevel();
				}
	        }
	    }
	}

}
