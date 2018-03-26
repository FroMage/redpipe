package net.redpipe.cdi;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.codec.BodyCodec;
import net.redpipe.engine.core.Server;

@RunWith(VertxUnitRunner.class)
public class AppTest {
    
    private Server server;
    private WebClient webClient;

    @Before
    public void before(TestContext context) {
        Async async = context.async();
        server = new Server();
        server.start().subscribe(v -> {
            System.err.println("Server started");
            webClient = WebClient.create(server.getVertx(),
                    new WebClientOptions().setDefaultHost("localhost").setDefaultPort(9000).setIdleTimeout(0));
            async.complete();
        }, x -> { 
            x.printStackTrace();
            context.fail(x);
            async.complete();
        });
    }
    
    @After
    public void after(TestContext context) {
        Async async = context.async();
        server.close(result -> {
            if(result.failed())
                context.fail(result.cause());
            async.complete();
        });
    }

    @Test
    public void testCdi(TestContext context) {
      final ConsoleHandler consoleHandler = new ConsoleHandler();
      consoleHandler.setLevel(Level.FINEST);
      consoleHandler.setFormatter(new SimpleFormatter());

      final Logger app = Logger.getLogger("org.jboss.weld.vertx");
      app.setLevel(Level.FINEST);
      app.addHandler(consoleHandler);

        Async async = context.async();
        webClient.get("/test")
        .as(BodyCodec.string())
        .rxSend()
        .subscribe(body -> {
            context.assertEquals(200, body.statusCode());
            context.assertEquals("hello", body.body());
            async.complete();
        }, x -> { 
            x.printStackTrace();
            context.fail(x);
            async.complete();
        });
    }
}