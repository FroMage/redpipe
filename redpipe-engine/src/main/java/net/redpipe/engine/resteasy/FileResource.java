package net.redpipe.engine.resteasy;

import java.io.File;
import java.io.IOException;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public abstract class FileResource {
	public Response getFile(@PathParam("path") String path) throws IOException{
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
}
