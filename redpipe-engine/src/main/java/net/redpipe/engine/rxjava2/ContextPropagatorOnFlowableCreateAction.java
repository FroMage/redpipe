package net.redpipe.engine.rxjava2;

import java.util.Map;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.Flowable;
import io.reactivex.functions.BiFunction;
import net.redpipe.engine.core.AppGlobals;

public class ContextPropagatorOnFlowableCreateAction
		implements BiFunction<Flowable, Subscriber, Subscriber> {

	@Override
	public Subscriber apply(Flowable flowable, Subscriber observer) throws Exception {
		return new ContextCapturerFlowable<>(flowable, observer);
	}

	public class ContextCapturerFlowable<T> implements Subscriber<T> {

	    private final Subscriber<T> source;
		private Map<Class<?>, Object> contextDataMap;
		private AppGlobals appGlobals;

		public ContextCapturerFlowable(Flowable<T> observable, Subscriber<T> observer) {
	    	this.source = observer;
	        contextDataMap = ResteasyProviderFactory.getContextDataMap();
	        appGlobals = AppGlobals.get();
		}

		@Override
		public void onComplete() {
			ResteasyProviderFactory.pushContextDataMap(contextDataMap);
			AppGlobals previous = AppGlobals.set(appGlobals);
			try {
	    		source.onComplete();
			}finally {
				AppGlobals.set(previous);
				ResteasyProviderFactory.removeContextDataLevel();
			}
		}

		@Override
		public void onError(Throwable t) {
			ResteasyProviderFactory.pushContextDataMap(contextDataMap);
			AppGlobals previous = AppGlobals.set(appGlobals);
			try {
	    		source.onError(t);
			}finally {
				AppGlobals.set(previous);
				ResteasyProviderFactory.removeContextDataLevel();
			}
		}

		@Override
		public void onNext(T v) {
			ResteasyProviderFactory.pushContextDataMap(contextDataMap);
			AppGlobals previous = AppGlobals.set(appGlobals);
			try {
	    		source.onNext(v);
			}finally {
				AppGlobals.set(previous);
				ResteasyProviderFactory.removeContextDataLevel();
			}
		}

		@Override
		public void onSubscribe(Subscription s) {
			ResteasyProviderFactory.pushContextDataMap(contextDataMap);
			AppGlobals previous = AppGlobals.set(appGlobals);
			try {
	    		source.onSubscribe(s);
			}finally {
				AppGlobals.set(previous);
				ResteasyProviderFactory.removeContextDataLevel();
			}
		}
	}

}
