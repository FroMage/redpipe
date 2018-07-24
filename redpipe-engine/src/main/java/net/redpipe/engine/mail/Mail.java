package net.redpipe.engine.mail;

import java.util.Map;

import javax.ws.rs.core.MediaType;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.reactivex.core.buffer.Buffer;
import net.redpipe.engine.core.AppGlobals;
import net.redpipe.engine.template.AbstractTemplate;
import net.redpipe.engine.template.TemplateRenderer;

public class Mail extends AbstractTemplate {

	String from;
	String[] to;
	String[] cc;
	String[] bcc;
	String subject;

	public Mail(String name, Map<String, Object> variables) {
		super(name, variables);
	}
	
	public Mail(String name) {
		super(name);
	}
	
	@Override
	public Mail set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	public Mail from(String address) {
		from = address;
		return this;
	}

	public Mail to(String... addresses) {
		to = addresses;
		return this;
	}

	public Mail cc(String... addresses) {
		cc = addresses;
		return this;
	}
	
	public Mail bcc(String... addresses) {
		bcc = addresses;
		return this;
	}

	public Mail subject(String subject) {
		this.subject = subject;
		return this;
	}

	public Completable send() {
		if(to == null && cc == null && bcc == null)
			throw new IllegalStateException("Missing to, cc or bcc");
		if(subject == null)
			throw new IllegalStateException("Missing subject");
		Mailer mailer = AppGlobals.get().getMailer();
		return mailer.send(this);
	}

	public Single<Buffer> renderText() {
		// FIXME: cache variants?
		return loadVariants()
				.flatMap(variants -> {
					System.err.println("Got variants for txt");
					String template = variants.getVariantTemplate(MediaType.TEXT_PLAIN_TYPE);
					TemplateRenderer renderer = AppGlobals.get().getTemplateRenderer(template);
					if(renderer == null)
						throw new RuntimeException("Failed to find template renderer for template "+template);
					return renderer.render(template, variables);
				}).map(response -> (Buffer)response.getEntity());
	}

	public Maybe<Buffer> renderHtml() {
		// FIXME: cache variants?
		return loadVariants()
				.flatMapMaybe(variants -> {
					System.err.println("Got variants for html");
					String template = variants.getVariantTemplate(MediaType.TEXT_HTML_TYPE, null);
					if(template == null)
						return Maybe.empty();
					TemplateRenderer renderer = AppGlobals.get().getTemplateRenderer(template);
					if(renderer == null)
						throw new RuntimeException("Failed to find template renderer for template "+template);
					return renderer.render(template, variables).toMaybe();
				}).map(response -> (Buffer)response.getEntity());
	}
}
