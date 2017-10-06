package org.vertxrs.coroutines;

import java.util.Map;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.vertxrs.db.SQLUtil;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.FiberExecutorScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableCallable;
import io.vertx.rxjava.core.Context;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.sql.SQLConnection;
import rx.Single;

public class Coroutines {

	private static final String FIBER_SCHEDULER_CONTEXT_KEY = "__vertx-sync.fiberScheduler";

	public interface SuspendableCallableWithConnection<T>  {
	    T run(SQLConnection c) throws SuspendExecution, InterruptedException;
	}
	
	@SuppressWarnings({ "serial" })
	@Suspendable
	public static <T> T await(Single<T> single) throws SuspendExecution{
		if(single == null)
			throw new NullPointerException();
		try {
			return new FiberAsync<T, Throwable>(){
				@Override
				protected void requestAsync() {
					single.subscribe(ret -> asyncCompleted(ret),
							t -> asyncFailed(t));	
				}
			}.run();
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> Single<T> fiber(SuspendableCallableWithConnection<T> body){
		return fiber(() -> {
			SQLConnection connection = await(SQLUtil.getConnection());
			try{
				return body.run(connection);
			}finally{
				connection.close();
			}
		});
	}
	
	public static <T> Single<T> fiber(SuspendableCallable<T> body){
		final Map<Class<?>, Object> contextDataMap = ResteasyProviderFactory.getContextDataMap();
		return Single.<T>create(sub -> {
			Fiber<T> fiber = new Fiber<T>(getContextScheduler(), () -> {
				try{
					// start by restoring the RE context in this Fiber's ThreadLocal
					ResteasyProviderFactory.pushContextDataMap(contextDataMap);
					T ret = body.run();
					if(!sub.isUnsubscribed())
						sub.onSuccess(ret);
				}catch(Throwable x){
					if(!sub.isUnsubscribed())
						sub.onError(x);
				}
			});
			fiber.start();
		});
	}

	public static FiberScheduler getContextScheduler() {
		Context context = Vertx.currentContext();
		if (context == null) {
			throw new IllegalStateException("Not in context");
		}
		if (!context.isEventLoopContext()) {
			throw new IllegalStateException("Not on event loop");
		}
		// We maintain one scheduler per context
		FiberScheduler scheduler = context.get(FIBER_SCHEDULER_CONTEXT_KEY);
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
			context.put(FIBER_SCHEDULER_CONTEXT_KEY, scheduler);
		}
		return scheduler;
	}

}
