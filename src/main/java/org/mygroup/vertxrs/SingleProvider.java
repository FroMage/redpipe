package org.mygroup.vertxrs;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.AsyncResponseProvider;

import rx.Single;
import rx.Subscription;

@Provider
public class SingleProvider implements AsyncResponseProvider<Single<?>>{

	private static class SingleAdaptor<T> extends CompletableFuture<T> 
	{
		private Subscription subscription;

		public SingleAdaptor(Single<T> observable) 
		{
			this.subscription = observable.subscribe(this::complete, this::completeExceptionally);
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning)
		{
			subscription.unsubscribe();
			return super.cancel(mayInterruptIfRunning);
		}
	}

	@Override
	public CompletionStage<?> toCompletionStage(Single<?> asyncResponse) {
		return new SingleAdaptor<>(asyncResponse);
	}

}
