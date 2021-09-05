package me.vinceh121.gmcserver.exceptions;

public class LimitReachedException extends Exception {
	private static final long serialVersionUID = 5661039714091334568L;

	public LimitReachedException() {
		super();
	}

	public LimitReachedException(String message, Throwable cause) {
		super(message, cause);
	}

	public LimitReachedException(String message) {
		super(message);
	}

	public LimitReachedException(Throwable cause) {
		super(cause);
	}
}
