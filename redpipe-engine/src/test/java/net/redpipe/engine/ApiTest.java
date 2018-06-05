package net.redpipe.engine;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import net.redpipe.engine.core.Server;

@RunWith(VertxUnitRunner.class)
public class ApiTest {

	private Server server;
	private WebClient webClient;

	@Before
	public void prepare(TestContext context) throws IOException {
		Async async = context.async();

		server = new Server();
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
		Async async = context.async();

		webClient
		.get("/inject")
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
	public void checkInjectRx1(TestContext context) {
		Async async = context.async();

		webClient
		.get("/rx1/inject")
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
}
