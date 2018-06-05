package net.redpipe.engine.rxjava;

import java.util.Map;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import net.redpipe.engine.core.AppGlobals;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func1;

public class ResteasyContextPropagatingOnObservableCreateAction implements Func1<OnSubscribe, OnSubscribe> {

	@Override
	public OnSubscribe call(OnSubscribe t) {
		return new ContextCapturerSingle(t);
	}
	
	final static class ContextCapturerSingle<T> implements OnSubscribe<T> {

		final Map<Class<?>, Object> contextDataMap;

	    final OnSubscribe<T> source;

		private AppGlobals appGlobals;

	    public ContextCapturerSingle(OnSubscribe<T> source) {
	        this.source = source;
	        contextDataMap = ResteasyProviderFactory.getContextDataMap();
	        appGlobals = AppGlobals.get();
	    }

	    @Override
	    public void call(Subscriber<? super T> t) {
	        source.call(new OnAssemblySingleSubscriber<T>(t, contextDataMap, appGlobals));
	    }

	    static final class OnAssemblySingleSubscriber<T> extends Subscriber<T> {

	        final Subscriber<? super T> actual;

	        final Map<Class<?>, Object> contextDataMap;

			private AppGlobals appGlobals;

	        public OnAssemblySingleSubscriber(Subscriber<? super T> actual, Map<Class<?>, Object> contextDataMap, AppGlobals appGlobals) {
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
	        public void onNext(T t) {
				ResteasyProviderFactory.pushContextDataMap(contextDataMap);
				AppGlobals previous = AppGlobals.set(appGlobals);
				try {
					actual.onNext(t);
				}finally {
					AppGlobals.set(previous);
					ResteasyProviderFactory.removeContextDataLevel();
				}
	        }

	        @Override
	        public void onCompleted() {
				ResteasyProviderFactory.pushContextDataMap(contextDataMap);
				AppGlobals previous = AppGlobals.set(appGlobals);
				try {
					actual.onCompleted();
				}finally {
					AppGlobals.set(previous);
					ResteasyProviderFactory.removeContextDataLevel();
				}
	        }
}
	}

}
