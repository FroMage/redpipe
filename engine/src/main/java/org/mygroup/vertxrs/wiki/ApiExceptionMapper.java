package org.mygroup.vertxrs.wiki;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.mygroup.vertxrs.ErrorMapper;

import io.vertx.core.json.JsonObject;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException>, ErrorMapper {

	@Override
	public Response toResponse(ApiException exception) {
		JsonObject response = new JsonObject();
		response
		.put("success", false)
		.put("error", exception.getMessage());
		return Response.status(exception.getStatus()).entity(response).build();
	}

	@Override
	public Response toErrorResponse(Throwable t) {
		JsonObject response = new JsonObject();
		response
		.put("success", false)
		.put("error", t.getMessage());
		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(response).build();
	}
}
