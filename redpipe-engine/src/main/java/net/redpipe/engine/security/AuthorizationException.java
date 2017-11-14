package net.redpipe.engine.security;

@SuppressWarnings("serial")
public class AuthorizationException extends RuntimeException {

	public AuthorizationException(String message) {
		super(message);
	}

}
