package net.redpipe.engine.rxjava2;

import java.util.Map;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.functions.Function;
import net.redpipe.engine.core.AppGlobals;

public class ContextPropagatorOnCompletableAssemblyAction implements Function<Completable, Completable> {

	@Override
	public Completable apply(Completable t) throws Exception {
		return new ContextPropagatorCompletable(t);
	}

	public class ContextPropagatorCompletable extends Completable {

		private Completable source;
		private Map<Class<?>, Object> contextDataMap;
		private AppGlobals appGlobals;

		public ContextPropagatorCompletable(Completable t) {
			this.source = t;
	        contextDataMap = ResteasyProviderFactory.getContextDataMap();
	        appGlobals = AppGlobals.get();
		}

		@Override
		protected void subscribeActual(CompletableObserver observer) {
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
