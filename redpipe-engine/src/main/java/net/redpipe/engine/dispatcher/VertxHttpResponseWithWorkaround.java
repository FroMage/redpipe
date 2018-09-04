package net.redpipe.engine.dispatcher;

import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.plugins.server.vertx.VertxHttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;

public class VertxHttpResponseWithWorkaround extends VertxHttpResponse {
	
	private HttpServerResponse response;
	
	public VertxHttpResponseWithWorkaround(HttpServerResponse response, ResteasyProviderFactory providerFactory) {
		super(response, providerFactory);
		this.response = response;
	}

	public VertxHttpResponseWithWorkaround(HttpServerResponse response, ResteasyProviderFactory providerFactory,
			HttpMethod method) {
		super(response, providerFactory, method);
		this.response = response;
	}

	@Override
	public void prepareChunkStream() {
		super.prepareChunkStream();

		// fix bugs in superclass
		if(isWithoutBody()) {
			response.setChunked(false);
			response.headersEndHandler(h -> {
				response.headers().remove(HttpHeaders.CONTENT_LENGTH);
				response.headers().remove(HttpHeaders.CONTENT_TYPE);
			});
		}
	}

	private boolean isWithoutBody() {
		int status = response.getStatusCode();
		return (status >= 100 && status < 200)
				|| status == Status.NO_CONTENT.getStatusCode()
				|| status == Status.NOT_MODIFIED.getStatusCode();
	}
}
