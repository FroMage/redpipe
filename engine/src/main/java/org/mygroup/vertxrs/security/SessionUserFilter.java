package org.mygroup.vertxrs.security;

import java.io.IOException;

import javax.annotation.Priority;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.mygroup.vertxrs.Config;
import org.mygroup.vertxrs.ResteasyFilterContext;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.auth.AuthProvider;
import io.vertx.rxjava.ext.auth.shiro.ShiroAuth;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.UserSessionHandler;

@Priority(Priorities.AUTHENTICATION)
@PreMatching
@Provider
public class SessionUserFilter implements ContainerRequestFilter {

	private UserSessionHandler userSessionHandler;
	private ShiroAuth auth;

	public SessionUserFilter() {
		Vertx vertx = ResteasyProviderFactory.getContextData(Vertx.class);
		JsonObject config = CDI.current().select(JsonObject.class, new AnnotationLiteral<Config>() {}).get();
		auth = ShiroAuth.create(vertx, new ShiroAuthOptions()
				  .setType(ShiroAuthRealmType.PROPERTIES)
				  .setConfig(new JsonObject()
				    .put("properties_path", config.getString("security_definitions"))));
		userSessionHandler = UserSessionHandler.create(auth);
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		ResteasyProviderFactory.pushContext(AuthProvider.class, auth);
		userSessionHandler.handle(RoutingContext.newInstance(new ResteasyFilterContext(requestContext)));
	}
}
