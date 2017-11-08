package org.vertxrs.example.wiki.shiroJdbc;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import io.vertx.core.json.JsonObject;

@Provider
public class ApiValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

	@Override
	public Response toResponse(ConstraintViolationException exception) {
		JsonObject response = new JsonObject();
		response
		.put("success", false)
		.put("error", "Bad request payload");
		return Response.status(Status.BAD_REQUEST).entity(response).build();
	}
}
