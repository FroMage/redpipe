package net.redpipe.fibers;

import java.util.Map;
import java.util.concurrent.Executor;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.reactivex.Single;
import io.vertx.reactivex.core.Context;
import io.vertx.reactivex.core.Vertx;
import net.redpipe.engine.core.AppGlobals;

public class JdkFibers {

//	public static <T> Fiber rawFiber(Runnable body){
//		final Map<Class<?>, Object> contextDataMap = ResteasyProviderFactory.getContextDataMap();
//		AppGlobals globals = AppGlobals.get();
//		return Fiber.execute(getContextScheduler(), () -> {
//			try{
//				// start by restoring the RE context in this Fiber's ThreadLocal
//				ResteasyProviderFactory.pushContextDataMap(contextDataMap);
//				AppGlobals.set(globals);
//				body.run();
//			}finally {
//				ResteasyProviderFactory.removeContextDataLevel();
//				AppGlobals.set(null);
//			}
//		});
//	}
//
//	public static Executor getContextScheduler() {
//		Context context = Vertx.currentContext();
//		if (context == null) {
////	        final Fiber parent = Fiber.currentFiber();
////	        if (parent == null)
////	            return DefaultFiberScheduler.getInstance();
////	        else
////	            return parent.getScheduler();
//			throw new IllegalStateException("Not in context");
//		}
//		if (!context.isEventLoopContext()) {
//			throw new IllegalStateException("Not on event loop");
//		}
//		// We maintain one scheduler per context
//		Executor scheduler = context.get(Fibers.FIBER_SCHEDULER_CONTEXT_KEY);
//		if (scheduler == null) {
//			Thread eventLoop = Thread.currentThread();
//			scheduler = command -> {
//				if (Thread.currentThread() != eventLoop) {
//					context.runOnContext(v -> command.run());
//				} else {
//					// Just run directly
//					command.run();
//				}
//			};
//			context.put(Fibers.FIBER_SCHEDULER_CONTEXT_KEY, scheduler);
//		}
//		return scheduler;
//	}
//
//	public static <V> void start(Runnable body) {
//		Fiber.execute(getContextScheduler(), body);
//	}
//
//	public static <T> T await(Single<T> single) throws Throwable {
//		Fiber fiber = (Fiber) Fiber.currentStrand();
//		Object[] val = new Object[1];
//		Throwable[] exc = new Throwable[1];
//		single.subscribe(ret -> {
//			val[0] = ret;
//			fiber.unpark();
//		},
//				t -> {
//					exc[0] = t;
//					fiber.unpark();
//				});	
//		Fiber.park();
//		if(exc[0] != null)
//			throw exc[0];
//		return (T) val[0];
//	}
//
}
