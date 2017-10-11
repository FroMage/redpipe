package org.vertxrs.engine.security;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AuthorizationExceptionMapper implements ExceptionMapper<AuthorizationException>{

	@Override
	public Response toResponse(AuthorizationException exception) {
		return Response.status(Status.FORBIDDEN).entity(exception.getMessage()).type(MediaType.TEXT_PLAIN_TYPE).build();
	}

}
