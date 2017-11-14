package net.redpipe.example.wiki.shiroJdbc;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import io.vertx.core.json.JsonObject;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

	@Override
	public Response toResponse(ApiException exception) {
		JsonObject response = new JsonObject();
		response
		.put("success", false)
		.put("error", exception.getMessage());
		return Response.status(exception.getStatus()).entity(response).build();
	}
}
