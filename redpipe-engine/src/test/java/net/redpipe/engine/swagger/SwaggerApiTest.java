package net.redpipe.engine.swagger;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import net.redpipe.engine.TestResource;
import net.redpipe.engine.TestResourceRxJava1;
import net.redpipe.engine.core.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(VertxUnitRunner.class)
public class SwaggerApiTest {

    private WebClient webClient;
    private Server server;

    @Before
    public void prepare(TestContext context) throws IOException {
        Async async = context.async();

        server = new Server();
        server.start(ReactiveController.class)
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
    public void checkSwagger(TestContext context) {
        Async async = context.async();
        webClient
                .get("/swagger.json")
                .as(BodyCodec.jsonObject())
                .rxSend()
                .map(r -> {
                    context.assertEquals(200, r.statusCode());
                    JsonObject swaggerDef = r.body();
                    context.assertNotNull(swaggerDef);
                    System.out.println(swaggerDef);
                    // general
                    context.assertEquals("2.0", swaggerDef.getString("swagger"));
                    // version
                    JsonObject info = swaggerDef.getJsonObject("info");
                    context.assertNotNull(info);
                    context.assertEquals("1.0", info.getString("version"));
                    // paths
                    JsonObject paths = swaggerDef.getJsonObject("paths");
                    context.assertNotNull(paths);
                    context.assertTrue(paths.size() > 0);
                    // host
                    context.assertNotNull(swaggerDef.getString("host"));
                    // definitions
                    JsonObject definitions = swaggerDef.getJsonObject("definitions");
                    context.assertNotNull(definitions);
                    context.assertTrue(definitions.size() == 1, "There should only be one definition. No Observable / Single / etc.");
                    JsonObject dogDef = definitions.getJsonObject("Dog");
                    context.assertNotNull(dogDef);
                    context.assertEquals("object", dogDef.getString("type"));
                    return r;
                })
                .subscribe(response -> async.complete());
    }



}
