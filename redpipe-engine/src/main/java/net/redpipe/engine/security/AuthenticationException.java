package net.redpipe.engine.security;

@SuppressWarnings("serial")
public class AuthenticationException extends RuntimeException {

	public AuthenticationException(String message) {
		super(message);
	}

}
