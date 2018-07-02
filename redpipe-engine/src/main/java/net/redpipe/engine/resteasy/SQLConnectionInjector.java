package net.redpipe.engine.resteasy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ContextInjector;

import io.reactivex.Single;
import io.vertx.reactivex.ext.sql.SQLConnection;
import net.redpipe.engine.core.AppGlobals;

@Provider
public class SQLConnectionInjector implements ContextInjector<Single<SQLConnection>, SQLConnection>{

	@Override
	public Single<SQLConnection> resolve(Class<? extends Single<SQLConnection>> rawType, Type genericType,
			Annotation[] annotations) {
		return AppGlobals.get().getDbConnection();
	}

}
