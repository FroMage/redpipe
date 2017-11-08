package org.vertxrs.example.wiki.keycloakJooq;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.vertxrs.engine.resteasy.FileResource;

import com.github.rjeschke.txtmark.Processor;

@Path("wiki/")
public class AppResource extends FileResource {
	@Path("app{path:(/.*)?}")
	@GET
	public Response get(@PathParam("path") String path) throws IOException{
		return super.getFile(path);
	}
	
	@Produces(MediaType.TEXT_HTML)
	@Path("app/markdown")
	@POST
	public Response markdown(String contents){
		return Response.ok(Processor.process(contents)).build();
	}
}
