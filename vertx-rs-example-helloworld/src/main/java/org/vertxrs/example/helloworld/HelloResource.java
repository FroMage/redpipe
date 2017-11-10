package org.vertxrs.example.helloworld;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.vertxrs.fibers.Fibers;

import co.paralleluniverse.fibers.Suspendable;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import rx.Single;

@Path("/")
public class HelloResource {
	@GET
	public String hello() {
		return "Hello World";
	}

	@Path("reactive")
	@GET
	public Single<String> helloReactive() {
		return Single.just("Hello Reactive World");
	}

	@Path("composed")
	@GET
	public Single<String> helloComposed(@Context Vertx vertx,
			@Context UriInfo uriInfo) {
		Single<String> request1 = get(vertx, getUri(uriInfo, null));
		Single<String> request2 = get(vertx, getUri(uriInfo, "helloReactive"));
			
		return request1.zipWith(request2, (hello1, hello2) -> hello1 + "\n" + hello2);
	}

	@Path("fiber")
	@GET
	public Single<String> helloFiber(@Context Vertx vertx,
			@Context UriInfo uriInfo) {
		return Fibers.fiber(() -> {
			String hello1 = Fibers.await(get(vertx, getUri(uriInfo, null)));
			String hello2 = Fibers.await(get(vertx, getUri(uriInfo, "helloReactive")));
			
			return hello1 + "\n" + hello2;
		});
	}

	private URI getUri(UriInfo uriInfo, String methodName) {
		UriBuilder builder = uriInfo.getBaseUriBuilder();
		if(methodName != null)
			builder.path(HelloResource.class, methodName);
		else
			builder.path(HelloResource.class);
		return builder.build();
	}
	
	private Single<String> get(Vertx vertx, URI uri){
		WebClient client = WebClient.create(vertx);
		Single<HttpResponse<Buffer>> responseHandler = 
				client.get(uri.getPort(), uri.getHost(), uri.getPath()).rxSend();

		return responseHandler.map(response -> response.body().toString());
	}
}
