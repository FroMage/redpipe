package net.redpipe.engine.mail;

import java.util.Arrays;
import java.util.Optional;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.mail.MailClient;

public class ProdMailer implements Mailer {

	private MailClient mailClient;

	public ProdMailer(Vertx vertx, JsonObject serverConfig) {
		MailConfig config = new MailConfig();
		config.setHostname(serverConfig.getString("smtp.hostname", MailConfig.DEFAULT_HOST));
		config.setPort(serverConfig.getInteger("smtp.port", MailConfig.DEFAULT_PORT));
		config.setUsername(serverConfig.getString("smtp.username"));
		config.setPassword(serverConfig.getString("smtp.password"));
		config.setKeepAlive(serverConfig.getBoolean("smtp.keepAlive", MailConfig.DEFAULT_KEEP_ALIVE));
		// make the default work on default linux installs
		config.setTrustAll(serverConfig.getBoolean("smtp.trustAll", true));
		config.setStarttls(StartTLSOptions.valueOf(serverConfig.getString("smtp.starttls", MailConfig.DEFAULT_TLS.name())));
		mailClient = MailClient.createShared(vertx, config);
	}
	
	@Override
	public Completable send(Mail email) {
		Single<Optional<Buffer>> htmlRender = email.renderHtml().map(buffer -> Optional.of(buffer)).toSingle(Optional.empty());
		Single<Buffer> textRender = email.renderText();
		return Single.zip(textRender, htmlRender, (text, html) -> {
					System.err.println("Got txt and html!");
					MailMessage message = new MailMessage();
					message.setFrom(email.from);
					if(email.to != null)
						message.setTo(Arrays.asList(email.to));
					if(email.cc != null)
						message.setCc(Arrays.asList(email.cc));
					if(email.bcc != null)
						message.setBcc(Arrays.asList(email.bcc));
					message.setSubject(email.subject);
					message.setText(text.toString());
					if(html.isPresent())
						message.setHtml(html.get().toString());
					System.err.println("Sending mail via PROD mailer");
					return mailClient.rxSendMail(message).ignoreElement();
				}).flatMapCompletable(c -> c);
	}
}
