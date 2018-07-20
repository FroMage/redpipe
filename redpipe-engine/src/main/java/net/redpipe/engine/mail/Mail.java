package net.redpipe.engine.mail;

import java.util.Map;

import io.reactivex.Completable;
import net.redpipe.engine.core.AppGlobals;
import net.redpipe.engine.template.Template;

public class Mail extends Template {

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
}
