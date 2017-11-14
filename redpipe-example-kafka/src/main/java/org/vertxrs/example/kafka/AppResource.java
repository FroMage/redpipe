package org.vertxrs.example.kafka;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.vertxrs.engine.resteasy.FileResource;

@Path("")
public class AppResource extends FileResource {
	@Path("static{path:(/.*)?}")
	@GET
	public Response get(@PathParam("path") String path) throws IOException{
		return super.getFile(path);
	}
}
