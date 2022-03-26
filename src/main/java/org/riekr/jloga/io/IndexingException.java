package org.riekr.jloga.io;

public class IndexingException extends RuntimeException {
	private static final long serialVersionUID = -645668282024058592L;

	public IndexingException() {}

	public IndexingException(String message) {
		super(message);
	}

	public IndexingException(String message, Throwable cause) {
		super(message, cause);
	}

	public IndexingException(Throwable cause) {
		super(cause);
	}

	public IndexingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
