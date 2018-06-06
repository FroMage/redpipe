package net.redpipe.engine.resteasy;

import java.io.IOException;

import javax.annotation.Priority;
import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.redpipe.engine.core.AppGlobals;

@Priority(0)
@Provider
@PreMatching
public class RxVertxProvider implements ContainerRequestFilter {

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		Vertx vertx = ResteasyProviderFactory.getContextData(io.vertx.core.Vertx.class);
		HttpServerRequest req = ResteasyProviderFactory.getContextData(HttpServerRequest.class);
		HttpServerResponse resp = ResteasyProviderFactory.getContextData(HttpServerResponse.class);
		
		// rx2
		ResteasyProviderFactory.pushContext(io.vertx.reactivex.core.Vertx.class, 
				io.vertx.reactivex.core.Vertx.newInstance(vertx));
		ResteasyProviderFactory.pushContext(io.vertx.reactivex.core.http.HttpServerRequest.class, 
				io.vertx.reactivex.core.http.HttpServerRequest.newInstance(req));
		ResteasyProviderFactory.pushContext(io.vertx.reactivex.core.http.HttpServerResponse.class, 
				io.vertx.reactivex.core.http.HttpServerResponse.newInstance(resp));
		// rx1
		ResteasyProviderFactory.pushContext(io.vertx.rxjava.core.Vertx.class, 
				io.vertx.rxjava.core.Vertx.newInstance(vertx));
		ResteasyProviderFactory.pushContext(io.vertx.rxjava.core.http.HttpServerRequest.class, 
				io.vertx.rxjava.core.http.HttpServerRequest.newInstance(req));
		ResteasyProviderFactory.pushContext(io.vertx.rxjava.core.http.HttpServerResponse.class, 
				io.vertx.rxjava.core.http.HttpServerResponse.newInstance(resp));

		ResteasyProviderFactory.pushContext(ServletContext.class, AppGlobals.get().getGlobal(ServletContext.class));
	}

}
