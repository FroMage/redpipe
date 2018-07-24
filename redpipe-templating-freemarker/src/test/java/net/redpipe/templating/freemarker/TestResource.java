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
		return Single.just(new Template("templates/index")
				.set("title", "my title")
				.set("message", "my message"));
	}

	@Path("indexWithTemplateExtension")
	@GET
	public Single<Template> indexWithTemplateExtension() {
		return Single.just(new Template("templates/index.ftl")
				.set("title", "my title")
				.set("message", "my message"));
	}

	@Path("nego")
	@GET
	public Single<Template> nego() {
		return Single.just(new Template("templates/nego")
				.set("title", "my title")
				.set("message", "my message"));
	}

	@Path("negoWithHtmlExtension")
	@GET
	public Single<Template> negoWithHtmlExtension() {
		return Single.just(new Template("templates/nego.html")
				.set("title", "my title")
				.set("message", "my message"));
	}

	@Path("negoWithHtmlAndTemplateExtension")
	@GET
	public Single<Template> negoWithHtmlAndTemplateExtension() {
		return Single.just(new Template("templates/nego.html.ftl")
				.set("title", "my title")
				.set("message", "my message"));
	}

	@Path("single")
	@GET
	public Single<Template> single() {
		return Single.just(new Template("templates/negoSingle")
				.set("title", "my title")
				.set("message", "my message"));
	}

	@Path("defaultTemplate")
	@GET
	public Single<Template> defaultTemplate() {
		return Single.just(new Template()
				.set("title", "my title")
				.set("message", "my message"));
	}

	@Path("mail")
	@GET
	public Single<Response> mail(){
		return new Mail("templates/mail")
			.set("title", "my title")
			.set("message", "my message")
			.to("foo@example.com")
			.from("foo@example.com")
			.subject("Test email")
			.send().toSingleDefault(Response.ok().build());
	}

	@Path("mail2")
	@GET
	public Single<Response> mail2(){
		return new Mail("templates/mail2")
			.set("title", "my title")
			.set("message", "my message")
			.to("foo@example.com")
			.from("foo@example.com")
			.subject("Test email")
			.send().toSingleDefault(Response.ok().build());
	}
}

