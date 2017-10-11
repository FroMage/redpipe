package org.vertxrs.engine.security;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.vertxrs.engine.core.AppGlobals;
import org.vertxrs.engine.resteasy.ResteasyFilterContext;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.JWTAuthHandler;

@Priority(Priorities.AUTHENTICATION+1)
@PreMatching
@Provider
public class SessionJWTUserFilter implements ContainerRequestFilter {

	private JWTAuthHandler jwtAuthHandler;
	private JWTAuth jwtAuth;

	public SessionJWTUserFilter(@Context AppGlobals globals, @Context Vertx vertx) {
		JsonObject config = globals.getConfig();
		JsonObject keyStoreOptions = new JsonObject().put("keyStore", config.getJsonObject("keystore"));

		// attempt to load a Key file
		jwtAuth = JWTAuth.create(vertx, keyStoreOptions);
		jwtAuthHandler = JWTAuthHandler.create(jwtAuth);
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		ResteasyProviderFactory.pushContext(JWTAuth.class, jwtAuth);
		// only filter if we have a header, otherwise it will try to force auth, regardless if whether
		// we want auth
		if(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION) != null)
			jwtAuthHandler.handle(RoutingContext.newInstance(new ResteasyFilterContext(requestContext)));
	}
}
