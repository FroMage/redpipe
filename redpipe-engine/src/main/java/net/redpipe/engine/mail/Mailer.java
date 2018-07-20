package net.redpipe.engine.mail;

import io.reactivex.Completable;

public interface Mailer {

	Completable send(Mail email);

}
