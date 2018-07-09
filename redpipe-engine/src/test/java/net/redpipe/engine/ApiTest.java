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
import io.vertx.reactivex.ext.auth.AuthProvider;
import io.vertx.reactivex.ext.auth.shiro.ShiroAuth;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.ext.web.handler.AuthHandler;
import io.vertx.reactivex.ext.web.handler.BasicAuthHandler;
import io.vertx.reactivex.ext.web.handler.UserSessionHandler;
import net.redpipe.engine.core.AppGlobals;
import net.redpipe.engine.core.Server;

@RunWith(VertxUnitRunner.class)
public class ApiTest {

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
		server.start(TestResource.class, TestResourceRxJava1.class, TestResourceEmpty.class)
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
		Async async = context.async();

		webClient
		.get("/does-not-exist")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(404, r.statusCode(), "status code is 404");
			return r.body();
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkPlainHello(TestContext context) {
		Async async = context.async();

		webClient
		.get("/hello")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals("hello", r.body());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkInject(TestContext context) {
		checkInject("", context);
	}
	
	@Test
	public void checkInjectRx1(TestContext context) {
		checkInject("/rx1", context);
	}

	private void checkInject(String prefix, TestContext context) {
		Async async = context.async();

		webClient
		.get(prefix+"/inject")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals("ok", r.body());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkInjectUser(TestContext context) {
		checkInjectUser("", context);
	}
	
	@Test
	public void checkInjectUserRx1(TestContext context) {
		checkInjectUser("/rx1", context);
	}

	private void checkInjectUser(String prefix, TestContext context) {
		Async async = context.async();

		webClient
		.get(prefix+"/inject-user")
		.putHeader(HttpHeaders.AUTHORIZATION, "Basic "+Base64.getEncoder().encodeToString("root:w00t".getBytes(Charset.forName("us-ascii"))))
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals("ok", r.body());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkInjectUserRequired(TestContext context) {
		checkInjectUserRequired("", context);
	}
	
	@Test
	public void checkInjectUserRequiredRx1(TestContext context) {
		checkInjectUserRequired("/rx1", context);
	}

	private void checkInjectUserRequired(String prefix, TestContext context) {
		Async async = context.async();

		webClient
		.get(prefix+"/inject-user")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.statusCode());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkInjectUserInvalid(TestContext context) {
		checkInjectUserInvalid("", context);
	}
	
	@Test
	public void checkInjectUserInvalidRx1(TestContext context) {
		checkInjectUserInvalid("/rx1", context);
	}

	private void checkInjectUserInvalid(String prefix, TestContext context) {
		Async async = context.async();

		webClient
		.get(prefix+"/inject-user")
		.putHeader(HttpHeaders.AUTHORIZATION, "Basic "+Base64.getEncoder().encodeToString("root:invalid".getBytes(Charset.forName("us-ascii"))))
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(Status.UNAUTHORIZED.getStatusCode(), r.statusCode());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Ignore("Requires https://github.com/resteasy/Resteasy/pull/1596 merged and released")
	@Test
	public void checkAuthRoleForbidden(TestContext context) {
		checkAuthRoleForbidden("", context);
	}
	
	@Ignore("Requires https://github.com/resteasy/Resteasy/pull/1596 merged and released")
	@Test
	public void checkAuthRoleForbiddenRx1(TestContext context) {
		checkAuthRoleForbidden("/rx1", context);
	}

	private void checkAuthRoleForbidden(String prefix, TestContext context) {
		Async async = context.async();

		webClient
		.get(prefix+"/auth-create")
		.putHeader(HttpHeaders.AUTHORIZATION, "Basic "+Base64.getEncoder().encodeToString("bar:gee".getBytes(Charset.forName("us-ascii"))))
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(Status.FORBIDDEN.getStatusCode(), r.statusCode());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkAuthRoleOK(TestContext context) {
		checkAuthRoleOK("", context);
	}
	
	@Test
	public void checkAuthRoleOKRx1(TestContext context) {
		checkAuthRoleOK("/rx1", context);
	}

	private void checkAuthRoleOK(String prefix, TestContext context) {
		Async async = context.async();

		webClient
		.get(prefix+"/auth-create")
		.putHeader(HttpHeaders.AUTHORIZATION, "Basic "+Base64.getEncoder().encodeToString("root:w00t".getBytes(Charset.forName("us-ascii"))))
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(Status.OK.getStatusCode(), r.statusCode());
			context.assertEquals("ok", r.body());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkAuthRoleCheckOK(TestContext context) {
		checkAuthRoleCheckOK("", context);
	}
	
	@Test
	public void checkAuthRoleCheckOKRx1(TestContext context) {
		checkAuthRoleCheckOK("/rx1", context);
	}

	private void checkAuthRoleCheckOK(String prefix, TestContext context) {
		Async async = context.async();

		webClient
		.get(prefix+"/auth-check")
		.putHeader(HttpHeaders.AUTHORIZATION, "Basic "+Base64.getEncoder().encodeToString("root:w00t".getBytes(Charset.forName("us-ascii"))))
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(Status.OK.getStatusCode(), r.statusCode());
			context.assertEquals("true", r.body());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkAuthRoleCheckDenied(TestContext context) {
		checkAuthRoleCheckDenied("", context);
	}
	
	@Test
	public void checkAuthRoleCheckDeniedRx1(TestContext context) {
		checkAuthRoleCheckDenied("/rx1", context);
	}

	private void checkAuthRoleCheckDenied(String prefix, TestContext context) {
		Async async = context.async();

		webClient
		.get(prefix+"/auth-check")
		.putHeader(HttpHeaders.AUTHORIZATION, "Basic "+Base64.getEncoder().encodeToString("bar:gee".getBytes(Charset.forName("us-ascii"))))
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(Status.OK.getStatusCode(), r.statusCode());
			context.assertEquals("false", r.body());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkHelloSingle(TestContext context) {
		checkHelloSingle("", context);
	}

	@Test
	public void checkHelloSingleRx1(TestContext context) {
		checkHelloSingle("/rx1", context);
	}

	private void checkHelloSingle(String prefix, TestContext context) {
		Async async = context.async();

		webClient
		.get(prefix+"/hello-single")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals("hello", r.body());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkHelloObservable(TestContext context) {
		checkHelloObservable("", context);
	}

	@Test
	public void checkHelloObservableRx1(TestContext context) {
		checkHelloObservable("/rx1", context);
	}
	
	private void checkHelloObservable(String prefix, TestContext context) {
		Async async = context.async();

		webClient
		.get(prefix+"/hello-observable")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals("onetwo", r.body());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
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
		checkCompletable("completable1", context);
	}

	@Test
	public void checkCompletableRx2(TestContext context) {
		checkCompletable("completable2", context);
	}

	@Test
	public void checkEmptyMaybe(TestContext context) {
		checkEmpty("maybe/empty", context, 404);
	}

	@Test
	public void checkFulfilledMaybe(TestContext context) {
		Async async = context.async();
		webClient
		.get("/empty/maybe/fulfilled")
		.rxSend()
		.doOnError(context::fail)
		.subscribe(resp -> {
			context.assertEquals(200, resp.statusCode());
			context.assertNotNull(resp.bodyAsString());
			async.complete();
		});
	}

	private void checkCompletable(String suffix, TestContext context) {
		checkEmpty(suffix, context, 204);
	}

	private void checkEmpty(String suffix, TestContext context, int expectedStatus) {
		Async async = context.async();
		webClient
		.get("/empty/" + suffix)
		.rxSend()
		.doOnError(context::fail)
		.subscribe(resp -> {
			context.assertEquals(expectedStatus, resp.statusCode());
			context.assertNull(resp.body());
			async.complete();
		});
	}
}
