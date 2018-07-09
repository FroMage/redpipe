package net.redpipe.engine;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.sse.SseEventSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.auth.AuthProvider;
import io.vertx.reactivex.ext.auth.shiro.ShiroAuth;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.ext.web.handler.AuthHandler;
import io.vertx.reactivex.ext.web.handler.BasicAuthHandler;
import io.vertx.reactivex.ext.web.handler.UserSessionHandler;
import net.redpipe.engine.core.AppGlobals;
import net.redpipe.engine.core.Server;

@RunWith(VertxUnitRunner.class)
public class ApiTest {

	private final static String IGNORE = "**IGNORE**";
	
	private Server server;
	private WebClient webClient;

	@Before
	public void prepare(TestContext context) throws IOException {
		Async async = context.async();

		server = new Server() {
			@Override
			protected AuthProvider setupAuthenticationRoutes() {
				AppGlobals globals = AppGlobals.get();
				AuthProvider auth = ShiroAuth.create(globals.getVertx(), new ShiroAuthOptions()
						.setType(ShiroAuthRealmType.PROPERTIES)
						.setConfig(new JsonObject()));
				
				globals.getRouter().route().handler(UserSessionHandler.create(auth));

				AuthHandler authHandler = BasicAuthHandler.create(auth);

				globals.getRouter().route().handler(context -> {
					// only filter if we have a header, otherwise it will try to force auth, regardless if whether
					// we want auth
					if(context.request().getHeader(HttpHeaders.AUTHORIZATION) != null) {
						// make sure we pause until we're ready to read
						context.request().pause();
						authHandler.handle(context);
					}else
						context.next();
				});
				globals.getRouter().route().handler(context -> {
					// unpause now that we have auth
					if(context.request().getHeader(HttpHeaders.AUTHORIZATION) != null) {
						context.request().resume();
					}
					context.next();
				});

				return auth;
			}

		};
		server.start(TestResource.class, TestResourceRxJava1.class)
		.subscribe(() -> {
			webClient = WebClient.create(server.getVertx(),
					new WebClientOptions().setDefaultHost("localhost").setDefaultPort(9000));
			async.complete();
		}, x -> {
			x.printStackTrace();
			context.fail(x);
			async.complete();
		});
	}

	@After
	public void finish(TestContext context) {
		webClient.close();
		Async async = context.async();
		server.close().subscribe(() -> async.complete(),
				x -> {
					context.fail(x); 
					async.complete();
				});
	}

	@Test
	public void checkSession(TestContext context) {
		Async async = context.async();

		webClient
		.get("/hello")
		.as(BodyCodec.string())
		.rxSend()
		.flatMap(r -> {
			String sessionCookie = r.getHeader("set-cookie");
			context.assertNotNull(sessionCookie, "must have a session");
			Cookie sessionCookieValue = ClientCookieDecoder.LAX.decode(sessionCookie);
			String sentSessionCookie = ClientCookieEncoder.LAX.encode(sessionCookieValue);
			return webClient.get("/hello")
					.putHeader("cookie", sentSessionCookie)
					.as(BodyCodec.string())
					.rxSend()
					.flatMap(r2 -> {
						String sessionCookie2 = r2.getHeader("set-cookie");
						context.assertNull(sessionCookie2, "reload does not recreate session");
						return webClient.get("/does-not-exist")
								.putHeader("cookie", sentSessionCookie)
								.as(BodyCodec.string())
								.rxSend();
					}).map(r2 -> {
						String sessionCookie2 = r2.getHeader("set-cookie");
						context.assertNull(sessionCookie2, "404 does not clear session");
						return r.body();
					});
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkErrorCodeRespected(TestContext context) {
		checkRequest(Status.NOT_FOUND.getStatusCode(), IGNORE, "/does-not-exist", context);
	}

	@Test
	public void checkPlainHello(TestContext context) {
		checkRequest(Status.OK.getStatusCode(), "hello", "/hello", context);
	}

	@Test
	public void checkInject(TestContext context) {
		checkRequest(Status.OK.getStatusCode(), "ok", "/inject", context);
	}
	
	@Test
	public void checkInjectRx1(TestContext context) {
		checkRequest(Status.OK.getStatusCode(), "ok", "/rx1/inject", context);
	}

	@Test
	public void checkInjectUser(TestContext context) {
		checkRequest(Status.OK.getStatusCode(), "ok", "/inject-user", context, toAuth("root:w00t"));
	}
	
	@Test
	public void checkInjectUserRx1(TestContext context) {
		checkRequest(Status.OK.getStatusCode(), "ok", "/rx1/inject-user", context, toAuth("root:w00t"));
	}

	@Test
	public void checkInjectUserRequired(TestContext context) {
		checkRequest(Status.UNAUTHORIZED.getStatusCode(), "User required", "/inject-user", context);
	}
	
	@Test
	public void checkInjectUserRequiredRx1(TestContext context) {
		checkRequest(Status.UNAUTHORIZED.getStatusCode(), "User required", "/rx1/inject-user", context);
	}

	@Test
	public void checkInjectUserInvalid(TestContext context) {
		checkRequest(Status.UNAUTHORIZED.getStatusCode(), "Unauthorized", "/inject-user", context, toAuth("root:invalid"));
	}
	
	@Test
	public void checkInjectUserInvalidRx1(TestContext context) {
		checkRequest(Status.UNAUTHORIZED.getStatusCode(), "Unauthorized", "/rx1/inject-user", context, toAuth("root:invalid"));
	}

	@Ignore("Requires https://github.com/resteasy/Resteasy/pull/1596 merged and released")
	@Test
	public void checkAuthRoleForbidden(TestContext context) {
		checkRequest(Status.FORBIDDEN.getStatusCode(), null, "/auth-create", context, toAuth("bar:gee"));
	}
	
	@Ignore("Requires https://github.com/resteasy/Resteasy/pull/1596 merged and released")
	@Test
	public void checkAuthRoleForbiddenRx1(TestContext context) {
		checkRequest(Status.FORBIDDEN.getStatusCode(), null, "/rx1/auth-create", context, toAuth("bar:gee"));
	}

	@Test
	public void checkAuthRoleOK(TestContext context) {
		checkRequest(200, "ok", "/auth-create", context, toAuth("root:w00t"));
	}
	
	@Test
	public void checkAuthRoleOKRx1(TestContext context) {
		checkRequest(200, "ok", "/rx1/auth-create", context, toAuth("root:w00t"));
	}

	@Test
	public void checkAuthRoleCheckOK(TestContext context) {
		checkRequest(200, "true", "/auth-check", context, toAuth("root:w00t"));
	}
	
	@Test
	public void checkAuthRoleCheckOKRx1(TestContext context) {
		checkRequest(200, "true", "/rx1/auth-check", context, toAuth("root:w00t"));
	}

	@Test
	public void checkAuthRoleCheckDenied(TestContext context) {
		checkRequest(200, "false", "/auth-check", context, toAuth("bar:gee"));
	}
	
	@Test
	public void checkAuthRoleCheckDeniedRx1(TestContext context) {
		checkRequest(200, "false", "/rx1/auth-check", context, toAuth("bar:gee"));
	}

	private String toAuth(String userPass) {
		return "Basic "+Base64.getEncoder().encodeToString(userPass.getBytes(Charset.forName("us-ascii")));
	}

	@Test
	public void checkHelloSingle(TestContext context) {
		checkRequest(200, "hello", "/hello-single", context);
	}

	@Test
	public void checkHelloSingleRx1(TestContext context) {
		checkRequest(200, "hello", "/rx1/hello-single", context);
	}

	@Test
	public void checkHelloObservable(TestContext context) {
		checkRequest(200, "onetwo", "/hello-observable", context);
	}

	@Test
	public void checkHelloObservableRx1(TestContext context) {
		checkRequest(200, "onetwo", "/rx1/hello-observable", context);
	}
	
	@Test
	public void checkHelloObservableCollect(TestContext context) {
		checkHelloObservableCollect("", context);
	}

	@Test
	public void checkHelloObservableCollectRx1(TestContext context) {
		checkHelloObservableCollect("/rx1", context);
	}
	
	private void checkHelloObservableCollect(String prefix, TestContext context) {
		Async async = context.async();

		webClient
		.get(prefix+"/hello-observable-collect")
		.as(BodyCodec.jsonArray())
		.rxSend()
		.map(r -> {
			context.assertEquals(new JsonArray().add("one").add("two"), r.body());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkHelloObservableSSE(TestContext context) {
		checkHelloObservableSSE("", context);
	}

	@Test
	public void checkHelloObservableSSERx1(TestContext context) {
		checkHelloObservableSSE("/rx1", context);
	}

	private void checkHelloObservableSSE(String prefix, TestContext context) {
		CountDownLatch latch = new CountDownLatch(2);
		Client client = ClientBuilder.newClient();
		WebTarget target = client.target("http://localhost:9000"+prefix+"/hello-observable-sse");
		SseEventSource msgEventSource = SseEventSource.target(target).build();
		try (SseEventSource eventSource = msgEventSource){
			eventSource.register(event -> {
				if(latch.getCount() == 2)
					context.assertEquals("one", event.readData(String.class));
				else
					context.assertEquals("two", event.readData(String.class));
				latch.countDown();
			}, ex -> {
				context.fail(ex);
			});
			eventSource.open();
			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void checkCompletableRx1(TestContext context) {
		checkRequest(204, null, "/rx1/completable", context);
	}

	@Test
	public void checkCompletableRx2(TestContext context) {
		checkRequest(204, null, "/completable", context);
	}

	@Test
	public void checkEmptyMaybe(TestContext context) {
		checkRequest(404, null, "/maybe-empty", context);
	}

	@Test
	public void checkFulfilledMaybe(TestContext context) {
		checkRequest(200, "something", "/maybe-fulfilled", context);
	}

	}

	private void checkRequest(int expectedStatus, String expectedBody, String url, TestContext context) {
		checkRequest(expectedStatus, expectedBody, url, context, null);
	}
	
	private void checkRequest(int expectedStatus, String expectedBody, String url, TestContext context, String authHeader) {
		Async async = context.async();

		HttpRequest<Buffer> request = webClient
		.get(url);
		
		if(authHeader != null)
			request.putHeader(HttpHeaders.AUTHORIZATION, authHeader);
		
		request.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(expectedStatus, r.statusCode());
			if(expectedBody != IGNORE)
				context.assertEquals(expectedBody, r.body());
			return r;
		})
		.doOnError(context::fail)
		.subscribe(response -> async.complete());
	}
}
