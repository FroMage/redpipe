package org.vertxrs.engine.rxjava;

import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.AsyncStreamProvider;
import org.reactivestreams.Publisher;

import rx.Observable;
import rx.RxReactiveStreams;

@Provider
public class ObservableProvider implements AsyncStreamProvider<Observable<?>>{

	@Override
	public Publisher<?> toAsyncStream(Observable<?> asyncResponse) {
		return RxReactiveStreams.toPublisher(asyncResponse);
	}

}
