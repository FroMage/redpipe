package net.redpipe.templating.freemarker;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.reactivex.Single;
import net.redpipe.engine.template.Template;

@Path("/")
public class TestResource {

	@GET
	public Single<Template> index() {
		return Single.just(new Template("templates/index.ftl")
				.set("title", "my title")
				.set("message", "my message"));
	}


}
