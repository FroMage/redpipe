package net.redpipe.discovery;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import net.redpipe.engine.core.Server;

@RunWith(VertxUnitRunner.class)
public class DiscoveryTest {

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
	public void checkServiceDiscovery(TestContext context) {
		Async async = context.async();

		webClient
		.get("/test/service-discovery")
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
	public void checkWebClient(TestContext context) {
		Async async = context.async();

		webClient
		.get("/test/web-client")
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
}
