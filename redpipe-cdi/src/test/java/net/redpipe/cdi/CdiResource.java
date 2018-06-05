package net.redpipe.cdi;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.jboss.weld.vertx.VertxConsumer;
import org.jboss.weld.vertx.VertxEvent;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;

@ApplicationScoped
@Path("test")
public class CdiResource {
	
	@Inject
	Vertx vertx;

	@Inject
	JsonObject config;
	
	@Path("injection")
	@GET
	public Response testGlobalInjection() {
		if(vertx != null && config != null
				// force CDI loading
				&& vertx.toString() != null && config.toString() != null)
			return Response.ok().build();
		return Response.serverError().build();
	}
	
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
