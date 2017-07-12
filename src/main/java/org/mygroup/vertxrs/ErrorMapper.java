package org.mygroup.vertxrs;

import javax.ws.rs.core.Response;

public interface ErrorMapper {
	public Response toErrorResponse(Throwable t);
}
