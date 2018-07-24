package net.redpipe.templating.freemarker;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.reactivex.Single;
import net.redpipe.engine.mail.Mail;
import net.redpipe.engine.template.Template;

@Path("/")
public class TestResource {

	@GET
	public Single<Template> index() {
		return Single.just(new Template("templates/index.ftl")
				.set("title", "my title")
				.set("message", "my message"));
	}
	
	@Path("nego")
	@GET
	public Single<Template> nego() {
		return Single.just(new Template("templates/nego.ftl")
				.set("title", "my title")
				.set("message", "my message"));
	}

	@Path("mail")
	@GET
	public Single<Response> mail(){
		return new Mail("templates/mail.ftl")
			.set("title", "my title")
			.set("message", "my message")
			.to("foo@example.com")
			.from("foo@example.com")
			.subject("Test email")
			.send().toSingleDefault(Response.ok().build());
	}
}
