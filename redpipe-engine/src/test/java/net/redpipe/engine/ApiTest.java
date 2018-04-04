package net.redpipe.engine;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import net.redpipe.engine.core.Server;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.CookieDecoder;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.codec.BodyCodec;

@RunWith(VertxUnitRunner.class)
public class ApiTest {

	private Server server;
	private WebClient webClient;
	
	@Before
	public void prepare(TestContext context) throws IOException {
		Async async = context.async();

		server = new Server();
		server.start(TestResource.class)
		.subscribe(v -> {
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
		// FIXME: doesn't use RxJava
		server.close(context.asyncAssertSuccess());
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

}
