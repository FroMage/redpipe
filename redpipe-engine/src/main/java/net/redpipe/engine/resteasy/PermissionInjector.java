package net.redpipe.engine.resteasy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ContextInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.web.RoutingContext;
import net.redpipe.engine.security.HasPermission;
import rx.Single;

@Provider
public class PermissionInjector implements ContextInjector<Single<Boolean>, Boolean>{

	@Override
	public Single<Boolean> resolve(Class<? extends Single<Boolean>> rawType, Type genericType,
			Annotation[] annotations) {
		for (Annotation annotation : annotations) {
			if(annotation.annotationType() == HasPermission.class) {
				RoutingContext ctx = ResteasyProviderFactory.getContextData(RoutingContext.class);
				User user = ctx.user();
				if(user == null)
					return Single.just(false);
				return user.rxIsAuthorised(((HasPermission) annotation).value());
			}
		}
		return null;
	}

}
