package org.mygroup.vertxrs;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rx.java.ObservableHandler;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.http.HttpServerResponse;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import rx.Observable;
import rx.Single;

@RequestScoped
@Path("/hello")
public class MyResource {
	
	@Inject
	private InjectedBean bean;

	@Inject @Config
	private JsonObject config;

	@GET
	public String hello(){
		return "hello bean: "+bean+", config: "+config;
	}

	@Path("2")
	@GET
	public Response hello2(){
		return Response.ok("hello 2").build();
	}
	
	@Path("3")
	@GET
	public void hello3(@Suspended final AsyncResponse asyncResponse,
		      // Inject the Vertx instance
		      @Context Vertx vertx){
		System.err.println("Creating client");
		HttpClientOptions options = new HttpClientOptions();
		options.setSsl(true);
		options.setTrustAll(true);
		options.setVerifyHost(false);
		HttpClient client = vertx.createHttpClient(options);
		client.getNow(443,
				"www.google.com", 
				"/robots.txt", 
				resp -> {
					System.err.println("Got response");
					resp.bodyHandler(body -> {
						System.err.println("Got body");
						asyncResponse.resume(Response.ok(body.toString()).build());
					});
				});
		System.err.println("Created client");
	}

	@Path("4")
	@GET
	public void hello4(@Suspended final AsyncResponse asyncResponse,
		      // Inject the Vertx instance
		      @Context Vertx vertx){
		System.err.println("Creating client");
		HttpClientOptions options = new HttpClientOptions();
		options.setSsl(true);
		options.setTrustAll(true);
		options.setVerifyHost(false);
		HttpClient client = vertx.createHttpClient(options);
		ObservableHandler<HttpClientResponse> responseHandler = RxHelper.observableHandler();
		client.getNow(443,
				"www.google.com", 
				"/robots.txt", 
				responseHandler.toHandler());
		
		ObservableHandler<Buffer> bodyHandler = RxHelper.observableHandler();
		responseHandler.subscribe(resp -> {
			System.err.println("Got response");
			resp.bodyHandler(bodyHandler.toHandler());
		});
		
		bodyHandler.subscribe(body -> {
			System.err.println("Got body");
			asyncResponse.resume(Response.ok(body.toString()).build());
		});
		System.err.println("Created client");
	}

	@Path("5")
	@GET
	public void hello5(@Suspended final AsyncResponse asyncResponse,
		      // Inject the Vertx instance
		      @Context Vertx vertx){
		io.vertx.rxjava.core.Vertx rxVertx = io.vertx.rxjava.core.Vertx.newInstance(vertx);
		System.err.println("Creating client");
		HttpClientOptions options = new HttpClientOptions();
		options.setSsl(true);
		options.setTrustAll(true);
		options.setVerifyHost(false);
		io.vertx.rxjava.core.http.HttpClient client = rxVertx.createHttpClient(options);
		// DOES NOT WORK: https://github.com/vert-x3/vertx-rx/issues/13
		Observable<io.vertx.rxjava.core.http.HttpClientResponse> responseHandler = client.get(443,
				"www.google.com", 
				"/robots.txt").toObservable();

		responseHandler.map(resp -> {
			System.err.println("Got response");
			return resp.toObservable(); 
		})
		.subscribe(body -> {
			System.err.println("Got body");
			asyncResponse.resume(Response.ok(body.toString()).build());
		});
		
		System.err.println("Created client");
	}

	@Path("6")
	@GET
	public void hello6(@Suspended final AsyncResponse asyncResponse,
		      // Inject the Vertx instance
		      @Context Vertx vertx){
		io.vertx.rxjava.core.Vertx rxVertx = io.vertx.rxjava.core.Vertx.newInstance(vertx);
		System.err.println("Creating client");
		WebClientOptions options = new WebClientOptions();
		options.setSsl(true);
		options.setTrustAll(true);
		options.setVerifyHost(false);
		WebClient client = WebClient.create(rxVertx, options);
		Single<HttpResponse<io.vertx.rxjava.core.buffer.Buffer>> responseHandler = client.get(443,
				"www.google.com", 
				"/robots.txt").rxSend();

		responseHandler.subscribe(body -> {
			System.err.println("Got body");
			asyncResponse.resume(Response.ok(body.body().toString()).build());
		});
		
		System.err.println("Created client");
	}

	// FIXME: Jax-rs 2.1?
	@Path("7")
	@GET
	@Async
	public CompletionStage<String> hello7(@Context Vertx vertx){
		io.vertx.rxjava.core.Vertx rxVertx = io.vertx.rxjava.core.Vertx.newInstance(vertx);
		System.err.println("Creating client");
		WebClientOptions options = new WebClientOptions();
		options.setSsl(true);
		options.setTrustAll(true);
		options.setVerifyHost(false);
		WebClient client = WebClient.create(rxVertx, options);
		Single<HttpResponse<io.vertx.rxjava.core.buffer.Buffer>> responseHandler = client.get(443,
				"www.google.com", 
				"/robots.txt").rxSend();

		CompletableFuture<String> ret = new CompletableFuture<>();
		responseHandler.subscribe(body -> {
			System.err.println("Got body");
			ret.complete(body.body().toString());
		});
		
		System.err.println("Created client");
		return ret;
	}

	@Path("7error")
	@GET
	@Async
	public CompletionStage<String> hello7Error(@Context Vertx vertx){
		io.vertx.rxjava.core.Vertx rxVertx = io.vertx.rxjava.core.Vertx.newInstance(vertx);
		System.err.println("Creating client");
		WebClientOptions options = new WebClientOptions();
		options.setSsl(true);
		options.setTrustAll(true);
		options.setVerifyHost(false);
		WebClient client = WebClient.create(rxVertx, options);
		Single<HttpResponse<io.vertx.rxjava.core.buffer.Buffer>> responseHandler = client.get(443,
				"www.google.com", 
				"/robots.txt").rxSend();

		CompletableFuture<String> ret = new CompletableFuture<>();
		responseHandler.subscribe(body -> {
			System.err.println("Got body");
			
			ret.completeExceptionally(new MyException());
		});
		System.err.println("Created client");
		return ret;
	}

	@Path("8")
	@GET
	@Async
	public Single<String> hello8(@Context io.vertx.rxjava.core.Vertx rxVertx){
		System.err.println("Creating client");
		WebClientOptions options = new WebClientOptions();
		options.setSsl(true);
		options.setTrustAll(true);
		options.setVerifyHost(false);
		WebClient client = WebClient.create(rxVertx, options);
		Single<HttpResponse<io.vertx.rxjava.core.buffer.Buffer>> responseHandler = client.get(443,
				"www.google.com", 
				"/robots.txt").rxSend();

		System.err.println("Created client");
		return responseHandler.map(body -> {
			System.err.println("Got body");
			return body.body().toString();
		});
	}

	@Path("8user")
	@Produces("text/json")
	@GET
	@Async
	public Single<User> hello8User(@Context io.vertx.rxjava.core.Vertx rxVertx){
		System.err.println("Creating client");
		WebClientOptions options = new WebClientOptions();
		options.setSsl(true);
		options.setTrustAll(true);
		options.setVerifyHost(false);
		WebClient client = WebClient.create(rxVertx, options);
		Single<HttpResponse<io.vertx.rxjava.core.buffer.Buffer>> responseHandler = client.get(443,
				"www.google.com", 
				"/robots.txt").rxSend();

		System.err.println("Created client");
		return responseHandler.map(body -> {
			System.err.println("Got body");
			return new User(body.body().toString());
		});
	}

	@Path("8error")
	@GET
	@Async
	public Single<String> hello8Error(@Context io.vertx.rxjava.core.Vertx rxVertx){
		System.err.println("Creating client");
		WebClientOptions options = new WebClientOptions();
		options.setSsl(true);
		options.setTrustAll(true);
		options.setVerifyHost(false);
		WebClient client = WebClient.create(rxVertx, options);
		Single<HttpResponse<io.vertx.rxjava.core.buffer.Buffer>> responseHandler = client.get(443,
				"www.google.com", 
				"/robots.txt").rxSend();

		System.err.println("Created client");
		return responseHandler.map(body -> {
			System.err.println("Got body");
			throw new MyException();
		});
	}

	@Path("9")
	@GET
	@Async
	public Observable<String> hello9(@Context io.vertx.rxjava.core.Vertx rxVertx){
		System.err.println("Creating timer");
		return rxVertx.periodicStream(1000).toObservable().map(r -> {
			System.err.println("Tick: "+r);
			return "Timer: "+System.currentTimeMillis();
		});
	}

	@Path("9nostream")
	@Produces("text/json")
	@GET
	@Async
	@CollectUntilComplete
	public Observable<String> hello9nostream(@Context io.vertx.rxjava.core.Vertx rxVertx){
		System.err.println("Creating timer");
		return rxVertx.periodicStream(1000).toObservable().map(r -> {
			System.err.println("Tick: "+r);
			return "Timer: "+System.currentTimeMillis();
		}).take(3);
	}

	@Path("9chunked")
	@Produces("text/json")
	@GET
	@Async
	@Chunked
	public Observable<String> hello9chunked(@Context io.vertx.rxjava.core.Vertx rxVertx){
		System.err.println("Creating timer");
		return rxVertx.periodicStream(1000).toObservable().map(r -> {
			System.err.println("Tick: "+r);
			return "Timer: "+System.currentTimeMillis();
		});
	}

	@Path("9error")
	@GET
	@Async
	public Observable<String> hello9Error(@Context io.vertx.rxjava.core.Vertx rxVertx){
		System.err.println("Creating timer");
		int[] i = new int[]{0};
		return rxVertx.periodicStream(1000).toObservable().map(r -> {
			System.err.println("Tick: "+r);
			if(i[0]++ < 5)
				return "Timer: "+System.currentTimeMillis();
			throw new MyException();
		});
	}

	@Produces("text/json")
	@Path("9user")
	@GET
	@Async
	public Observable<User> hello9User(@Context io.vertx.rxjava.core.Vertx rxVertx){
		System.err.println("Creating timer");
		return rxVertx.periodicStream(1000).toObservable().map(r -> {
			System.err.println("Tick: "+r);
			return new User("Timer: "+System.currentTimeMillis());
		});
	}

	@VertxInject
	private String connection;
	
	@Path("10")
	@GET
	@Async
	@Interceptors(MyCdiInterceptor.class)
	@MyCdiIntercept
	public Single<String> hello10(@Context io.vertx.rxjava.core.Vertx rxVertx){
		System.err.println("Creating timer: "+connection);
		return rxVertx.timerStream(1000).toObservable().toSingle().map(r -> {
			System.err.println("Tick: "+r);
			return "Timer: "+System.currentTimeMillis();
		});
	}

//	@Path("6")
//	@GET
//	@Async
//	public Response helloAsync(@Context Vertx vertx){
//		System.err.println("Creating client");
//		HttpClientOptions options = new HttpClientOptions();
//		options.setSsl(true);
//		options.setTrustAll(true);
//		options.setVerifyHost(false);
//		HttpClient client = vertx.createHttpClient(options);
//		
//		HttpClientResponse response = client.getNow(443,
//				"www.google.com", 
//				"/robots.txt").await();
//		System.err.println("Got response");
//		
//		Buffer body = response.body().await();
//		System.err.println("Got body");
//		
//		return Response.ok(body.toString()).build();
//	}
}
