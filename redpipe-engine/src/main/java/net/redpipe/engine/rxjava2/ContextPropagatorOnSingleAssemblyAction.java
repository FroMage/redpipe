package net.redpipe.engine.rxjava2;

import java.util.Map;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.functions.Function;
import net.redpipe.engine.core.AppGlobals;

public class ContextPropagatorOnSingleAssemblyAction implements Function<Single, Single> {

	@Override
	public Single apply(Single t) throws Exception {
		return new ContextPropagatorSingle(t);
	}

	public class ContextPropagatorSingle<T> extends Single<T> {

		private Single<T> source;
		private Map<Class<?>, Object> contextDataMap;
		private AppGlobals appGlobals;

		public ContextPropagatorSingle(Single<T> t) {
			this.source = t;
	        contextDataMap = ResteasyProviderFactory.getContextDataMap();
	        appGlobals = AppGlobals.get();
		}

		@Override
		protected void subscribeActual(SingleObserver<? super T> observer) {
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
