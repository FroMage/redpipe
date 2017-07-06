package org.mygroup.vertxrs;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import javax.enterprise.inject.spi.CDI;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyAsynchronousResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.vertx.rxjava.core.Vertx;

@Interceptor
public class MyCdiInterceptor {

	@AroundInvoke
	public Object callAsync(final InvocationContext ctx) throws Exception {
		System.err.println("Intercepting start");
		Vertx vertx = ResteasyProviderFactory.getContextData(io.vertx.rxjava.core.Vertx.class);
		Class<? extends Object> klass = ctx.getTarget().getClass().getSuperclass();
		// FIXME: better injection facility
		for (Field field : klass.getDeclaredFields()) {
			System.err.println("field: "+field.getAnnotations().length);
			if(field.isAnnotationPresent(VertxInject.class)){
				System.err.println("Doing injection ourselves");
				HttpRequest req = ResteasyProviderFactory.getContextData(org.jboss.resteasy.spi.HttpRequest.class);
				Map<Class<?>, Object> contextData = ResteasyProviderFactory.getContextDataMap();
				ResteasyAsynchronousResponse asynchronousResponse = req.getAsyncContext().suspend();
				vertx.timerStream(1000).toObservable().subscribe(r -> {
					System.err.println("Ready to do injection");
					field.setAccessible(true);
					try {
						field.set(ctx.getTarget(), "yes!");
					} catch (IllegalArgumentException | IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						ResteasyProviderFactory.pushContextDataMap(contextData);
						Object ret = ctx.proceed();
						// pass this on to AsyncValidator
						CDI.current().select(AsyncValidator.class).get().handleReturnValue(ret, req, asynchronousResponse);
						ResteasyProviderFactory.removeContextDataLevel();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.err.println("Intercepting end");
				});
				// abort
				System.err.println("Delaying interception");
				return null;
			}
		}
		try{
			return ctx.proceed();
		}finally{
			System.err.println("Intercepting end");
		}
	}

}
