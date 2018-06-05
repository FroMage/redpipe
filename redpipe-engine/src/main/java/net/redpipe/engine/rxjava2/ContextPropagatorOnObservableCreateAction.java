package net.redpipe.engine.rxjava2;

import java.util.Map;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import net.redpipe.engine.core.AppGlobals;

public class ContextPropagatorOnObservableCreateAction
		implements BiFunction<Observable, Observer, Observer> {

	@Override
	public Observer apply(Observable observable, Observer observer) throws Exception {
		return new ContextCapturerObservable(observable, observer);
	}

	public class ContextCapturerObservable<T> implements Observer<T> {

	    private final Observer<T> source;
		private Map<Class<?>, Object> contextDataMap;
		private AppGlobals appGlobals;

		public ContextCapturerObservable(Observable<T> observable, Observer<T> observer) {
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
	}
}
