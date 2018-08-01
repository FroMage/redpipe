package net.redpipe.fibers;

import java.util.Map;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.github.fromage.quasi.fibers.Suspendable;

import io.reactivex.Single;
import io.vertx.reactivex.ext.sql.SQLConnection;
import net.redpipe.engine.core.AppGlobals;
import net.redpipe.engine.db.SQLUtil;

public class Fibers {

	static final String FIBER_SCHEDULER_CONTEXT_KEY = "__vertx-sync.fiberScheduler";

	public interface SuspendableCallableWithConnection<T>  {
		@Suspendable
	    T run(SQLConnection c) throws Exception;
	}

	public interface SuspendableCallable<T>  {
		@Suspendable
	    T run() throws Exception;
	}

	@Suspendable
	public static <T> T await(Single<T> single) throws Exception{
		if(single == null)
			throw new NullPointerException();
		try {
			return QuasiFibers.await(single);
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
		AppGlobals globals = AppGlobals.get();
		return Single.<T>create(sub -> {
			QuasiFibers.start(() -> {
				try{
					// start by restoring the RE context in this Fiber's ThreadLocal
					ResteasyProviderFactory.pushContextDataMap(contextDataMap);
					AppGlobals.set(globals);
					T ret = body.run();
					if(!sub.isDisposed())
						sub.onSuccess(ret);
				}catch(Throwable x){
					if(!sub.isDisposed())
						sub.onError(x);
				}finally {
					ResteasyProviderFactory.removeContextDataLevel();
					AppGlobals.set(null);
				}
				return null;
			});
		});
	}


}
