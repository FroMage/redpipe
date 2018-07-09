package net.redpipe.engine;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.annotations.Stream;
import org.jboss.resteasy.annotations.Stream.MODE;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.auth.AuthProvider;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.Session;
import net.redpipe.engine.security.HasPermission;
import net.redpipe.engine.security.NoAuthRedirect;
import net.redpipe.engine.security.RequiresPermissions;
import net.redpipe.engine.security.RequiresUser;

@Path("/")
public class TestResource {

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

	@Path("hello")
	@GET
	public String hello() {
		return "hello";
	}

	@Path("hello-single")
	@GET
	public Single<String> helloSingle() {
		return Single.just("hello");
	}

	@Stream(MODE.RAW)
	@Path("hello-observable")
	@GET
	public Observable<String> helloObservable() {
		return Observable.fromArray("one", "two");
	}

	@Produces(MediaType.APPLICATION_JSON)
	@Path("hello-observable-collect")
	@GET
	public Observable<String> helloObservableCollect() {
		return Observable.fromArray("one", "two");
	}

	
	@Produces(MediaType.SERVER_SENT_EVENTS)
	@Path("hello-observable-sse")
	@GET
	public Observable<String> helloObservableSse() {
		return Observable.fromArray("one", "two");
	}

	@NoAuthRedirect
	@RequiresUser
	@Path("inject-user")
	@GET
	public String injectUser(@Context User user,
			@Context AuthProvider authProvider) {
		if(user == null
				|| authProvider == null)
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		return "ok";
	}

	@NoAuthRedirect
	@RequiresPermissions("create")
	@Path("auth-create")
	@GET
	public Single<String> authCreate() {
		return Single.just("ok").delay(1, TimeUnit.SECONDS);
	}

	@NoAuthRedirect
	@RequiresUser
	@Path("auth-check")
	@GET
	public Single<Boolean> authCheck(@Context User user,
			@Context @HasPermission("create") boolean second) {
		return user.rxIsAuthorised("create").map(first -> first && second);
	}
	
    @GET
    @Path("completable")
    public Completable returnCompletable() {
        return Completable.complete(); // should be 204
    }
    
    @GET
    @Path("maybe-empty")
    public Maybe<String> returnEmptyMaybe() {
        return Maybe.empty(); // should be 404
    }

    @GET
    @Path("maybe-fulfilled")
    public Maybe<String> returnFulfilledMaybe() {
        return Maybe.just("something"); // should be 200
    }
}
