package me.vinceh121.gmcserver.exceptions;

public class EntityNotFoundException extends Exception {
	private static final long serialVersionUID = -8257871569216329644L;

	public EntityNotFoundException() {
		super();
	}

	public EntityNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public EntityNotFoundException(String message) {
		super(message);
	}

	public EntityNotFoundException(Throwable cause) {
		super(cause);
	}
}
