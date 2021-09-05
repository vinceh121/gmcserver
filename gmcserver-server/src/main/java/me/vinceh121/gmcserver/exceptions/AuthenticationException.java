package me.vinceh121.gmcserver.exceptions;

public class AuthenticationException extends Exception {
	private static final long serialVersionUID = -8257871569216329644L;

	public AuthenticationException() {
		super();
	}

	public AuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}

	public AuthenticationException(String message) {
		super(message);
	}

	public AuthenticationException(Throwable cause) {
		super(cause);
	}
}
