package net.redpipe.fibers;

import java.util.Map;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.github.fromage.quasi.fibers.Fiber;
import com.github.fromage.quasi.fibers.FiberAsync;
import com.github.fromage.quasi.fibers.FiberExecutorScheduler;
import com.github.fromage.quasi.fibers.FiberScheduler;
import com.github.fromage.quasi.fibers.Suspendable;
import com.github.fromage.quasi.strands.SuspendableCallable;

import io.reactivex.Single;
import io.vertx.reactivex.core.Context;
import io.vertx.reactivex.core.Vertx;
import net.redpipe.engine.core.AppGlobals;

public class QuasiFibers {

	public static <T> Fiber<T> rawFiber(SuspendableCallable<T> body){
		final Map<Class<?>, Object> contextDataMap = ResteasyProviderFactory.getContextDataMap();
		AppGlobals globals = AppGlobals.get();
		return new Fiber<T>(getContextScheduler(), () -> {
			try{
				// start by restoring the RE context in this Fiber's ThreadLocal
				ResteasyProviderFactory.pushContextDataMap(contextDataMap);
				AppGlobals.set(globals);
				return body.run();
			}finally {
				ResteasyProviderFactory.removeContextDataLevel();
				AppGlobals.set(null);
			}
		});
	}

	public static FiberScheduler getContextScheduler() {
		Context context = Vertx.currentContext();
		if (context == null) {
//	        final Fiber parent = Fiber.currentFiber();
//	        if (parent == null)
//	            return DefaultFiberScheduler.getInstance();
//	        else
//	            return parent.getScheduler();
			throw new IllegalStateException("Not in context");
		}
		if (!context.isEventLoopContext()) {
			throw new IllegalStateException("Not on event loop");
		}
		// We maintain one scheduler per context
		FiberScheduler scheduler = context.get(Fibers.FIBER_SCHEDULER_CONTEXT_KEY);
		if (scheduler == null) {
			Thread eventLoop = Thread.currentThread();
			scheduler = new FiberExecutorScheduler("vertx.contextScheduler", command -> {
				if (Thread.currentThread() != eventLoop) {
					context.runOnContext(v -> command.run());
				} else {
					// Just run directly
					command.run();
				}
			});
			context.put(Fibers.FIBER_SCHEDULER_CONTEXT_KEY, scheduler);
		}
		return scheduler;
	}

	public static <V> void start(SuspendableCallable<V> body) {
		new Fiber<V>(getContextScheduler(), body).start();
	}

	@Suspendable
	public static <T> T await(Single<T> single) throws Throwable {
		return new FiberAsync<T, Throwable>(){
			@Override
			protected void requestAsync() {
				single.subscribe(ret -> asyncCompleted(ret),
						t -> asyncFailed(t));	
			}
		}.run();
	}

}
