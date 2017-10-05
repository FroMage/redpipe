package org.mygroup.vertxrs.wiki;

import java.io.File;
import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.github.rjeschke.txtmark.Processor;

@Path("wiki/")
public class AppResource {
	@Path("app{path:(/.*)?}")
	@GET
	public Response get(@PathParam("path") String path) throws IOException{
		// TODO: add caching support with precondition
		File root = new File("src/main/resources/webroot");
		File f = new File(root, path);
		// FIXME: check that this is enough
		if(!f.getCanonicalPath().startsWith(root.getCanonicalPath()+"/"))
			return Response.status(404).build();
		if(f.isDirectory())
			return Response.ok("Folder "+path).build();
		if(!f.exists())
			return Response.status(Status.NOT_FOUND).build();
		return Response.ok(f).build();
	}
	
	@Produces(MediaType.TEXT_HTML)
	@Path("app/markdown")
	@POST
	public Response markdown(String contents){
		return Response.ok(Processor.process(contents)).build();
	}
}
