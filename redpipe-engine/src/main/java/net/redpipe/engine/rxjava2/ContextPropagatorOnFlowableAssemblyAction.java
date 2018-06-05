package net.redpipe.engine.rxjava2;

import java.util.Map;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import net.redpipe.engine.core.AppGlobals;

public class ContextPropagatorOnFlowableAssemblyAction implements Function<Flowable, Flowable> {

	@Override
	public Flowable apply(Flowable t) throws Exception {
		return new ContextPropagatorFlowable(t);
	}

	public class ContextPropagatorFlowable<T> extends Flowable<T> {

		private Flowable<T> source;
		private Map<Class<?>, Object> contextDataMap;
		private AppGlobals appGlobals;

		public ContextPropagatorFlowable(Flowable<T> t) {
			this.source = t;
	        contextDataMap = ResteasyProviderFactory.getContextDataMap();
	        appGlobals = AppGlobals.get();
		}

		@Override
		protected void subscribeActual(Subscriber<? super T> observer) {
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
