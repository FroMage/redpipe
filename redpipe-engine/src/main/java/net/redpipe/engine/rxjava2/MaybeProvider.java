package net.redpipe.engine.rxjava2;

import io.reactivex.Maybe;
import io.reactivex.disposables.Disposable;
import org.jboss.resteasy.spi.AsyncResponseProvider;

import javax.ws.rs.core.Response;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class MaybeProvider implements AsyncResponseProvider<Maybe<?>> {

    private static class MaybeAdaptor<T> extends CompletableFuture<Object>
    {
        private Disposable subscription;

        public MaybeAdaptor(Maybe<T> observable)
        {
            this.subscription = observable.subscribe(this::complete, this::completeExceptionally, this::completeEmpty);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            subscription.dispose();
            return super.cancel(mayInterruptIfRunning);
        }

        void completeEmpty() {
            super.complete(Response.status(404).build());
        }

    }

    @Override
    public CompletionStage<?> toCompletionStage(Maybe<?> asyncResponse) {
        return new MaybeAdaptor<>(asyncResponse);
    }

}
