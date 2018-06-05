package net.redpipe.engine.rxjava2;

import java.util.Map;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.functions.Function;
import net.redpipe.engine.core.AppGlobals;

public class ContextPropagatorOnObservableAssemblyAction implements Function<Observable, Observable> {

	@Override
	public Observable apply(Observable t) throws Exception {
		return new ContextPropagatorObservable(t);
	}

	public class ContextPropagatorObservable<T> extends Observable<T> {

		private Observable<T> source;
		private Map<Class<?>, Object> contextDataMap;
		private AppGlobals appGlobals;

		public ContextPropagatorObservable(Observable<T> t) {
			this.source = t;
	        contextDataMap = ResteasyProviderFactory.getContextDataMap();
	        appGlobals = AppGlobals.get();
		}

		@Override
		protected void subscribeActual(Observer<? super T> observer) {
			ResteasyProviderFactory.pushContextDataMap(contextDataMap);
			AppGlobals previous = AppGlobals.set(appGlobals);
			try {
				source.subscribe(observer);
			}finally {
				AppGlobals.set(previous);
				ResteasyProviderFactory.removeContextDataLevel();
			}
		}

	}

}
