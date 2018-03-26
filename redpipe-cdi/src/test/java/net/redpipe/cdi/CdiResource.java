package net.redpipe.cdi;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.jboss.weld.vertx.VertxConsumer;
import org.jboss.weld.vertx.VertxEvent;

import io.vertx.rxjava.core.Vertx;
import rx.Single;

@ApplicationScoped
@Path("test")
public class CdiResource {
	
    @GET
    public Single<Response> test(@Context Vertx vertx) {
        return vertx.eventBus()
            .rxSend("echo.address", "hello")
            .map(msg -> {
                System.err.println("Got reply: "+msg.body());
                return Response.ok(msg.body()).build();
            });
    }

    void echoConsumer(@Observes @VertxConsumer("echo.address") VertxEvent event) {
        System.err.println("Got event: "+event.getMessageBody());
        event.setReply(event.getMessageBody()+" response from event bus");
    }
}
