package net.redpipe.templating.freemarker;

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
		server.start(TestResource.class)
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
	public void checkTemplate(TestContext context) {
		Async async = context.async();

		webClient
		.get("/")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			context.assertEquals(200, r.statusCode());
			context.assertEquals("<html>\n" + 
					" <head>\n" + 
					"  <title>my title</title>\n" + 
					" </head>\n" + 
					" <body>my message</body>\n" + 
					"</html>", r.body());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkTemplateNoRenderer(TestContext context) {
		Async async = context.async();

		webClient
		.get("/no-renderer")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			System.err.println(r.body());
			context.assertEquals(500, r.statusCode());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}

	@Test
	public void checkTemplateMissing(TestContext context) {
		Async async = context.async();

		webClient
		.get("/missing")
		.as(BodyCodec.string())
		.rxSend()
		.map(r -> {
			System.err.println(r.body());
			context.assertEquals(500, r.statusCode());
			return r;
		})
		.doOnError(x -> context.fail(x))
		.subscribe(response -> {
			async.complete();
		});
	}
}
