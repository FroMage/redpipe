package net.redpipe.engine.rxjava2;

import java.util.Map;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import net.redpipe.engine.core.AppGlobals;

public class ContextPropagatorOnSingleCreateAction implements BiFunction<Single, SingleObserver, SingleObserver> {

	@Override
	public SingleObserver apply(Single s, SingleObserver o) throws Exception {
		return new ContextCapturerSingle(s, o);
	}

	final static class ContextCapturerSingle<T> implements SingleObserver<T> {

	    private final SingleObserver<T> source;
		private Map<Class<?>, Object> contextDataMap;
		private AppGlobals appGlobals;

	    public ContextCapturerSingle(Single<T> s, SingleObserver<T> o) {
	    	this.source = o;
	        contextDataMap = ResteasyProviderFactory.getContextDataMap();
	        appGlobals = AppGlobals.get();
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
