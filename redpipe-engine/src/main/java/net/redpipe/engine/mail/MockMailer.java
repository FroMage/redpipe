package net.redpipe.engine.mail;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.core.buffer.Buffer;

public class MockMailer implements Mailer {

	private Map<String, List<SentMail>> sentMails = new HashMap<>();
	
	public MockMailer() {
		System.err.println("new MockMailer "+System.identityHashCode(this));
	}

	@Override
	public Completable send(Mail email) {
		Single<Optional<Buffer>> htmlRender = email.renderHtml().map(buffer -> Optional.of(buffer)).toSingle(Optional.empty());
		Single<Buffer> textRender = email.renderText();
		return Single.zip(textRender, htmlRender, (text, html) -> {
					send(email, text, html.orElse(null));
					return Completable.complete();
				}).flatMapCompletable(c -> c);
	}

	private void send(Mail email, Buffer text, Buffer html) {
		SentMail sentMail = new SentMail(email, text.toString(), html != null ? html.toString() : null);
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
		public final String html;
		public final Mail email;

		public SentMail(Mail email, String text, String html) {
			this.email = email;
			this.text = text;
			this.html = html;
		}

	}

}
