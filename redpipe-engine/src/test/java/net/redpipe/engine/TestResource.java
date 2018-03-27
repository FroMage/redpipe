package net.redpipe.engine;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class TestResource {
	@Path("hello")
	@GET
	public String hello() {
		return "hello";
	}
}
