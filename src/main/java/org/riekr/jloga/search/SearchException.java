package org.riekr.jloga.search;

import java.util.concurrent.atomic.AtomicBoolean;

public class SearchException extends RuntimeException {
	private static final long serialVersionUID = 3324262040130284875L;

	public final AtomicBoolean userHasAlreadyBeenNotified = new AtomicBoolean(false);

	public SearchException() {
		userHasAlreadyBeenNotified.set(true);
	}

	public SearchException(String message) {
		this(message, false);
	}

	public SearchException(String message, boolean userHasAlreadyBeenNotified) {
		super(message);
		this.userHasAlreadyBeenNotified.set(userHasAlreadyBeenNotified);
	}

	public SearchException(String message, Throwable cause) {
		super(message, cause);
		userHasAlreadyBeenNotified.set(false);
	}

}
