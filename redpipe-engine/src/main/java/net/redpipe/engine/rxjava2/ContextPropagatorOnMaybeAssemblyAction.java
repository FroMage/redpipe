package net.redpipe.engine.rxjava2;

import java.util.Map;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.functions.Function;
import net.redpipe.engine.core.AppGlobals;

public class ContextPropagatorOnMaybeAssemblyAction implements Function<Maybe, Maybe> {

	@Override
	public Maybe apply(Maybe t) throws Exception {
		return new ContextPropagatorMaybe(t);
	}

	public class ContextPropagatorMaybe<T> extends Maybe<T> {

		private Maybe<T> source;
		private Map<Class<?>, Object> contextDataMap;
		private AppGlobals appGlobals;

		public ContextPropagatorMaybe(Maybe<T> t) {
			this.source = t;
	        contextDataMap = ResteasyProviderFactory.getContextDataMap();
	        appGlobals = AppGlobals.get();
		}

		@Override
		protected void subscribeActual(MaybeObserver<? super T> observer) {
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
