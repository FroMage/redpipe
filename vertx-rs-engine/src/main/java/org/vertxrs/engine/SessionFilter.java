package org.vertxrs.engine;

import static io.vertx.core.http.HttpHeaders.SET_COOKIE;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.vertxrs.resteasy.ResteasyFilterContext;

import io.vertx.ext.web.Cookie;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.SessionHandler;
import io.vertx.rxjava.ext.web.sstore.LocalSessionStore;

@Priority(Priorities.AUTHENTICATION - 500)
@PreMatching
@Provider
public class SessionFilter implements ContainerRequestFilter, ContainerResponseFilter {

	private SessionHandler sessionHandler;
	
	public SessionFilter() {
		Vertx vertx = AppGlobals.get().getVertx();
		System.err.println("vertx: "+vertx);
		sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx));
	}
	
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
	}

	private void saveCookies(RoutingContext routingContext) {
		for (Cookie cookie : routingContext.getDelegate().cookies()) {
			if(cookie.isChanged()) {
				// NOTE: at this point it's too late to fill in the jax-rs response with NewCookie
				// because they've already been collected and passed to Vertx, so we have to deal with Vertx headers
				routingContext.response().headers().add(SET_COOKIE.toString(), cookie.encode());
			}
		}
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		RoutingContext routingContext = ResteasyProviderFactory.getContextData(RoutingContext.class);
		routingContext.addHeadersEndHandler(x -> saveCookies(routingContext));
		wrapCookies(routingContext, requestContext.getCookies());
		sessionHandler.handle(RoutingContext.newInstance(new ResteasyFilterContext(requestContext)));
	}

	private void wrapCookies(RoutingContext routingContext, Map<String, javax.ws.rs.core.Cookie> map) {
		for (Entry<String, javax.ws.rs.core.Cookie> entry : map.entrySet()) {
			javax.ws.rs.core.Cookie cookie = entry.getValue();
			Cookie wrapped = Cookie.cookie(cookie.getName(), cookie.getValue());
			wrapped.setDomain(cookie.getDomain());
			wrapped.setPath(cookie.getPath());
			wrapped.setChanged(false);
			routingContext.getDelegate().addCookie(wrapped);
		}
	}
}
