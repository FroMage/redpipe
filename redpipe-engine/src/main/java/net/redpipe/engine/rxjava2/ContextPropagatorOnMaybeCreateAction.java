package net.redpipe.engine.rxjava2;

import java.util.Map;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import net.redpipe.engine.core.AppGlobals;

public class ContextPropagatorOnMaybeCreateAction
		implements BiFunction<Maybe, MaybeObserver, MaybeObserver> {

	@Override
	public MaybeObserver apply(Maybe maybe, MaybeObserver observer) throws Exception {
		return new ContextCapturerMaybe<>(maybe, observer);
	}

	public class ContextCapturerMaybe<T> implements MaybeObserver<T> {

	    private final MaybeObserver<T> source;
		private Map<Class<?>, Object> contextDataMap;
		private AppGlobals appGlobals;

		public ContextCapturerMaybe(Maybe<T> observable, MaybeObserver<T> observer) {
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
		public void onSubscribe(Disposable d) {
			ResteasyProviderFactory.pushContextDataMap(contextDataMap);
			AppGlobals previous = AppGlobals.set(appGlobals);
			try {
	    		source.onSubscribe(d);
			}finally {
				AppGlobals.set(previous);
				ResteasyProviderFactory.removeContextDataLevel();
			}
		}

		@Override
		public void onSuccess(T v) {
			ResteasyProviderFactory.pushContextDataMap(contextDataMap);
			AppGlobals previous = AppGlobals.set(appGlobals);
			try {
	    		source.onSuccess(v);
			}finally {
				AppGlobals.set(previous);
				ResteasyProviderFactory.removeContextDataLevel();
			}
		}
	}

}
