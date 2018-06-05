package net.redpipe.engine;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.annotations.Stream;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.auth.AuthProvider;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.Session;
import rx.Observable;
import rx.Single;


@Path("/rx1")
public class TestResourceRxJava1 {

	@Path("inject")
	@GET
	public String inject(@Context Vertx vertx,
			@Context RoutingContext routingContext,
			@Context HttpServerRequest request,
			@Context HttpServerResponse response,
			@Context AuthProvider authProvider,
			@Context User user,
			@Context Session session) {
		if(vertx == null
				|| routingContext == null
				|| request == null
				|| response == null
				|| session == null)
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		return "ok";
	}

	@Path("hello-single")
	@GET
	public Single<String> helloSingle() {
		return Single.just("hello");
	}

	@Stream
	@Path("hello-observable")
	@GET
	public Observable<String> helloObservable() {
		return Observable.just("one", "two");
	}

	@Produces(MediaType.APPLICATION_JSON)
	@Path("hello-observable-collect")
	@GET
	public Observable<String> helloObservableCollect() {
		return Observable.just("one", "two");
	}

	
	@Produces(MediaType.SERVER_SENT_EVENTS)
	@Path("hello-observable-sse")
	@GET
	public Observable<String> helloObservableSse() {
		return Observable.just("one", "two");
	}
}
