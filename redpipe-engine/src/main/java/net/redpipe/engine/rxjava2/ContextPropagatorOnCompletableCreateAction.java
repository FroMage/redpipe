package net.redpipe.engine.rxjava2;

import java.util.Map;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import net.redpipe.engine.core.AppGlobals;

public class ContextPropagatorOnCompletableCreateAction
		implements BiFunction<Completable, CompletableObserver, CompletableObserver> {

	@Override
	public CompletableObserver apply(Completable completable, CompletableObserver observer) throws Exception {
		return new ContextCapturerCompletable(completable, observer);
	}

	final static class ContextCapturerCompletable implements CompletableObserver {

	    private final CompletableObserver source;
		private Map<Class<?>, Object> contextDataMap;
		private AppGlobals appGlobals;

	    public ContextCapturerCompletable(Completable s, CompletableObserver o) {
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
	}

}
