package org.vertxrs.security;

@SuppressWarnings("serial")
public class AuthenticationException extends RuntimeException {

	public AuthenticationException(String message) {
		super(message);
	}

}
