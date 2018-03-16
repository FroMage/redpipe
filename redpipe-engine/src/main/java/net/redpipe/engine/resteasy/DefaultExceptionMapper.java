package net.redpipe.engine.resteasy;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Throwable> {

	@Override
	public Response toResponse(Throwable exception) {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		exception.printStackTrace(printWriter);
		// FIXME: this should really end up in a template.
		String text = 
				"<html>"+
						" <head><title>Error: "+exception.getMessage()+"</title></head>"+
						" <body style='background: #ff8572;'><p style='font-weight: bold; padding: 20px; font-size: 16pt; '>Error: "+exception.getMessage()+"</p>"+
						" <pre style='background: #ffface; padding: 20px;'>"+writer.toString()+
						" </pre><body></html>";
		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(text).type(MediaType.TEXT_HTML).build();
	}

}
