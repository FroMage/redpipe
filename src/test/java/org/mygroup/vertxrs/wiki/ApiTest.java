package org.mygroup.vertxrs.wiki;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mygroup.vertxrs.Server;

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
	public void prepare(TestContext context) {
	    JsonObject config = new JsonObject()
	    	      .put("db_url", "jdbc:hsqldb:mem:testdb;shutdown=true")
	    	      .put("max_pool", 4);

		// FIXME: doesn't use RxJava
		server = new Server();
		server.start(config, context.asyncAssertSuccess());
		
	    webClient = WebClient.create(server.getVertx(), new WebClientOptions()
	    	      .setDefaultHost("localhost")
	    	      .setDefaultPort(9000));
	}
	
	@After
	public void finish(TestContext context) {
		// FIXME: doesn't use RxJava
		server.close(context.asyncAssertSuccess());
	}

	@Test
	public void playWithApi(TestContext context) {
		  Async async = context.async();

		  JsonObject page = new JsonObject()
		    .put("name", "Sample")
		    .put("markdown", "# A page");

		  webClient.post("/wiki/api/pages")
		    .as(BodyCodec.jsonObject())
		    .rxSendJsonObject(page)
		    .map(r -> r.body())
		    .flatMap(response -> {
		    	System.err.println(response);
			    context.assertTrue(response.getBoolean("success"));
		    	return webClient.get("/wiki/api/pages")
				    .as(BodyCodec.jsonObject())
				    .rxSendJsonObject(page)
				    .map(r -> r.body());
		    })
		    .flatMap(response -> {
			    context.assertTrue(response.getBoolean("success"));
			    JsonArray array = response.getJsonArray("pages");
			    context.assertEquals(1, array.size());
			    context.assertEquals(0, array.getJsonObject(0).getInteger("id"));
		    	return webClient.put("/wiki/api/pages/0")
					    .as(BodyCodec.jsonObject())
					    .rxSendJsonObject(new JsonObject()
						        .put("id", 0)
						        .put("markdown", "Oh Yeah!"))
					    .map(r -> r.body());
		    })
		    .flatMap(response -> {
			    context.assertTrue(response.getBoolean("success"));
		    	return webClient.delete("/wiki/api/pages/0")
					    .as(BodyCodec.jsonObject())
					    .rxSendJsonObject(new JsonObject()
						        .put("id", 0)
						        .put("markdown", "Oh Yeah!"))
					    .map(r -> r.body());
		    })
		    .doOnError(x -> context.fail(x))
		    .subscribe(response -> {
		    	context.assertTrue(response.getBoolean("success"));
		    	async.complete();
		    });
		}

}
