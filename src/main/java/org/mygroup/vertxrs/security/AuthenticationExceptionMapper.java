package org.mygroup.vertxrs.security;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AuthenticationExceptionMapper implements ExceptionMapper<AuthenticationException>{

	@Override
	public Response toResponse(AuthenticationException exception) {
		return Response.status(Status.UNAUTHORIZED).entity(exception.getMessage()).type(MediaType.TEXT_PLAIN_TYPE).build();
	}

}
