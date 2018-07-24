package net.redpipe.engine.mail;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import io.reactivex.Completable;
import io.vertx.reactivex.core.buffer.Buffer;

public class MockMailer implements Mailer {

	private Map<String, List<SentMail>> sentMails = new HashMap<>();

	@Override
	public Completable send(Mail email) {
		return email.render(null).flatMapCompletable(response -> {
			send(email, response);
			return Completable.complete();
		});
	}

	private void send(Mail email, Response response) {
		// FIXME: multipart
		String text = ((Buffer) response.getEntity()).toString();
		SentMail sentMail = new SentMail(email, text);
		System.err.println("Sending mail via MOCK mailer");
		if (email.to != null) {
			for (String to : email.to) {
				send(sentMail, to);
			}
		}
		if (email.cc != null) {
			for (String to : email.cc) {
				send(sentMail, to);
			}
		}
		if (email.bcc != null) {
			for (String to : email.bcc) {
				send(sentMail, to);
			}
		}
	}

	private void send(SentMail sentMail, String to) {
		List<SentMail> mails = sentMails.get(to);
		if (mails == null) {
			mails = new LinkedList<>();
			sentMails.put(to, mails);
		}
		mails.add(sentMail);
	}

	public List<SentMail> getMailsSentTo(String address) {
		return sentMails.get(address);
	}

	public static class SentMail {

		public final String text;
		public final Mail email;

		public SentMail(Mail email, String text) {
			this.email = email;
			this.text = text;
		}

	}

}
