package net.redpipe.prototyping;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import net.redpipe.engine.core.Server;
import net.redpipe.fibers.Fibers;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;

public class Main {
	private static void log(String s) {
//		System.err.println("[debug] "+s);
	}
	
	public static void main(String[] args) throws IOException {
		Server test = new Server();
		test.start();
	}
	
	private static void testForeach() {
		Observable<String> obs = emittingFiber(observer -> {
			List<String> values = new LinkedList<>(Arrays.asList("a", "b", "c"));
			log("In emitting fiber");
			while(!values.isEmpty()) {
				log("In emitting onNext");
				observer.onNext(values.remove(0));
				log("In emitting onNext done");
			}
			log("In emitting onCompleted");
			observer.onCompleted();
			log("In emitting onCompleted done");
		});
		obs.subscribe(System.err::println, Throwable::printStackTrace);
		
		CompletableFuture<Void> future = new CompletableFuture<Void>();
		Fibers.fiber(() -> {
			log("Iterating fiber 1");
			for(String s : toIterable(obs)) {
				System.err.println(s);
				log("Got value: "+s+", sleeping");
				Fiber.sleep(1000);
				log("Sleeping done");
			}
			return null;
		}).subscribe(v -> future.complete(null),
				x -> future.completeExceptionally(x));
		try {
			future.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		log("Main done");
	}
	
	public static <T> Observable<T> emitterToObservable(EmittingFunction<T> f){
		return Observable.unsafeCreate(observer -> {
			SuspendingObserverImpl<T> suspendingObserver = new SuspendingObserverImpl<T>(observer);
			f.startEmitting(suspendingObserver);
		});
	}

	public static <T> Observable<T> emittingFiber(EmittingFunction<T> f){
		return emitterToObservable(emitter -> {
			Fibers.rawFiber(() -> {
				log("Emitting fiber start");
				f.startEmitting(emitter);
				log("Emitting fiber done");
				return null;
			}).start();
		});
	}

	public static class SuspendingObserverImpl<T> implements SuspendingObserver<T> {

		private Subscriber<? super T> observer;
		private long requested;
		private Fiber<?> waiter;

		public SuspendingObserverImpl(Subscriber<? super T> observer) {
			this.observer = observer;
			observer.setProducer(n -> request(n));
		}

		public void request(long n) {
			if(n == 0)
				return;
			requested += n;
			if(waiter != null) {
				log("Unparking observer");
				waiter.unpark(this);
			}
		}
		
		@Suspendable
		@Override
		public void onCompleted() {
			// FIXME: lock
			waitForReady();
			observer.onCompleted();
		}

		@Suspendable
		@Override
		public void onError(Throwable e) {
			// FIXME: lock
			waitForReady();
			observer.onError(e);
		}

		@Suspendable
		@Override
		public void onNext(T t) {
			// FIXME: lock
			waitForReady();
			requested--;
			observer.onNext(t);
		}


		@Suspendable
		private void waitForReady() {
			while(requested == 0 && !observer.isUnsubscribed()) {
				try {
					waiter = Fiber.currentFiber();
					log("Observer not ready: parking");
					Fiber.park(this);
					log("Observer not ready: parking done");
					waiter = null;
				} catch (SuspendExecution e) {
					throw new AssertionError(e);
				}
			}
			if(observer.isUnsubscribed())
				throw new RuntimeException("Nobody cares");
		}
		
	}
	
	@FunctionalInterface
	public interface EmittingFunction<T> {
		@Suspendable
		void startEmitting(SuspendingObserver<T> obs);
	}
	
	public interface SuspendingObserver<T> extends Observer<T> {
		@Suspendable
		@Override
		void onCompleted();
		
		@Suspendable
		@Override
		void onError(Throwable e);
		
		@Suspendable
		@Override
		void onNext(T t);
	}
	
	public static <T> SuspendableIterable<T> toIterable(Observable<T> obs){
		return new SuspendableIterable<T>() {
			@Override
			public SuspendableIterator<T> iterator() {
				return new SuspendableIteratorImpl<T>(obs);
			}
		};
	}
	
	static class SuspendableIteratorImpl<T> implements SuspendableIterator<T> {

		private T next;
		private Throwable error;
		private boolean done;
		private Fiber<?> waiting;
		private Subscription sub;
		private SuspendableIteratorImpl<T>.MySusbscriber subscriber;
		
		class MySusbscriber extends Subscriber<T>{
			
			public void publicRequest(long n) {
				log("publicRequest: "+n);
				request(n);
			}
			
			@Override
			public void onStart() {
				log("onStart");
				request(0);
			}
			
			@Override
			public void onCompleted() {
				log("onCompleted");
				SuspendableIteratorImpl.this.done = true;
				// don't clear next because we get next and completed in the same go,
				// before we consume the last element
				waiting.unpark(SuspendableIteratorImpl.this);
			}

			@Override
			public void onError(Throwable e) {
				log("onError: "+e);
				SuspendableIteratorImpl.this.next = null;
				SuspendableIteratorImpl.this.error = e;
				waiting.unpark(SuspendableIteratorImpl.this);
			}

			@Override
			public void onNext(T t) {
				log("onNext: "+t);
				SuspendableIteratorImpl.this.next = t;
				SuspendableIteratorImpl.this.error = null;
				// FIXME: check that we didn't already have a value
				waiting.unpark(SuspendableIteratorImpl.this);
			}
		}

		public SuspendableIteratorImpl(Observable<T> obs) {
			subscriber = new MySusbscriber();
			sub = obs.subscribe(subscriber);
			waiting = Fiber.currentFiber();
		}

		@Suspendable
		@Override
		public T next() {
			do {
				log("next() next: "+next+", error: "+error+", done: "+done);
				// FIXME: allow null next values
				if(next != null) {
					T ret = next;
					next = null;
					return ret;
				}
				if(error != null)
					rethrow(error);
				if(done)
					throw new NoSuchElementException();
				try {
					log("next request");
					subscriber.publicRequest(1);
					log("next park");
					Fiber.park(this);
					log("next park done");
				} catch (SuspendExecution e) {
					throw new AssertionError(e);
				}
			}while(true);
		}

		private <E extends Throwable> void rethrow(Throwable e) throws E {
			throw (E) e;
		}

		@Suspendable
		@Override
		public boolean hasNext() {
			while(!done && next == null && error == null) {
				try {
					log("hasNext request");
					subscriber.publicRequest(1);
					log("hasNext park");
					Fiber.park(this);
					log("hasNext park done");
				} catch (SuspendExecution e) {
					throw new AssertionError(e);
				}
			}
			log("hasNext next: "+next+", error: "+error+", done: "+done);
			// FIXME: allow null next values
			if(next != null) {
				return true;
			}
			// FIXME: or return yes and throw from next() ?
			if(error != null)
				rethrow(error);
			if(done)
				return false;
			throw new AssertionError("Can't happen");
		}
		
	}
	
	interface SuspendableIterable<T> extends Iterable<T> {
		@Override
		SuspendableIterator<T> iterator();
	}

	interface SuspendableIterator<T> extends Iterator<T>{
		@Override
		@Suspendable
		T next();

		@Override
		@Suspendable
		boolean hasNext();
	}
}
