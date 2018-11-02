package net.redpipe.engine;

import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.reactivex.annotations.NonNull;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.AuthProvider;
import io.vertx.reactivex.ext.auth.shiro.ShiroAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.ext.web.handler.AuthHandler;
import io.vertx.reactivex.ext.web.handler.BasicAuthHandler;
import io.vertx.reactivex.ext.web.handler.UserSessionHandler;
import net.redpipe.engine.core.AppGlobals;
import net.redpipe.engine.core.Server;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;

@RunWith(VertxUnitRunner.class)
public class ServerExtendableTest {

    private Server server;
    private WebClient webClient;

    @Before
    public void startVertxWithConfiguredVertxOptions(TestContext context) throws IOException {
        Async async = context.async();

        server = new Server() {
            @Override
            protected @NonNull
            VertxOptions configureVertxOptions(VertxOptions options) {
                return options.setWorkerPoolSize(3);
            }

            @Override
            protected AuthProvider setupAuthenticationRoutes() {
                Router router = AppGlobals.get().getRouter();
                router.get("/test").handler(context -> {
                    context.response().end("OK");
                });
                return super.setupAuthenticationRoutes();
            }
        };
        server.start(new JsonObject().put("sessionDisabled", true), TestResource.class, TestResourceRxJava1.class)
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
    public void checkHasNoSession(TestContext context) {
        Async async = context.async();

        webClient
                .get("/test")
                .as(BodyCodec.string())
                .rxSend()
                .map(r -> {
                    String sessionCookie = r.getHeader("set-cookie");
                    context.assertNull(sessionCookie, "shouldn't have a session");
                    return r.body();
                })
                .doOnError(x -> context.fail(x))
                .subscribe(response -> {
                    async.complete();
                });
    }
}
