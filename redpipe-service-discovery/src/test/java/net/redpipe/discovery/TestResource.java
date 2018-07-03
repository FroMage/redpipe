package net.redpipe.discovery;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;

import io.reactivex.Single;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.servicediscovery.Record;

@ServiceName("test-service")
@Path("/test")
public class TestResource {

	@ServiceName("hello-service")
	@Path("hello")
	@GET
	public String hello() {
		return "hello";
	}

	@Path("service-discovery")
	@GET
	public String serviceDiscovery(@Context @ServiceName("test-service") Record classRecord,
			@Context @ServiceName("hello-service") Record methodRecord,
			@Context @ServiceName("missing-service") Record missingRecord) {
		if(classRecord == null || !classRecord.getLocation().getString("endpoint").equals("http://localhost:9000/test"))
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		if(methodRecord == null || !methodRecord.getLocation().getString("endpoint").equals("http://localhost:9000/test/hello"))
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		if(missingRecord != null)
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		return "ok";
	}

	@Path("web-client")
	@GET
	public Single<String> webClient(@Context @ServiceName("hello-service") WebClient client,
			// OK that's super lame
			@Context @ServiceName("hello-service") Record record) {
		if(client == null)
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		return client.get(record.getLocation().getString("root"))
				.as(BodyCodec.string())
				.rxSend()
				.map(response -> response.body())
				.doFinally(() -> client.close());
	}
}
