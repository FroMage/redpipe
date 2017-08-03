package org.mygroup.vertxrs.security;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.shiro.authz.UnauthorizedException;

@Provider
public class ShiroExceptionMapper implements ExceptionMapper<UnauthorizedException>{

	@Override
	public Response toResponse(UnauthorizedException exception) {
		return Response.status(Status.FORBIDDEN).build();
	}

}
