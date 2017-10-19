package org.vertxrs.example.kafka;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.vertxrs.engine.core.AppGlobals;

import io.vertx.core.json.JsonObject;
import rx.Observable;

@Path("/sse")
public class KafkaResource {

	@GET
	@Produces(MediaType.SERVER_SENT_EVENTS)
	public Observable<JsonObject> index(@Context AppGlobals globals){
		UUID uuid = UUID.randomUUID();
		Observable<JsonObject> consumer = (Observable<JsonObject>) globals.getGlobal("consumer");
	    Observable<JsonObject> ret = consumer
	        .buffer(1, TimeUnit.SECONDS)
	        .map((List<JsonObject> metrics) -> {
	          System.err.println("Metrics for "+uuid);
	          JsonObject dashboard = new JsonObject();
	          for (JsonObject metric : metrics) {
	            dashboard.mergeIn(metric);
	          }
	          return dashboard;
	    }).doOnUnsubscribe(() -> {
	    	System.err.println("Unsub for "+uuid);
	    });
	    
	    return ret;
	}

}
