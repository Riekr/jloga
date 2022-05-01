package org.riekr.jloga.transform;

public class ColumnsChangedException extends IllegalArgumentException {
	private static final long serialVersionUID = -5930076906336264785L;

	public ColumnsChangedException(int from, int to, int line) {
		super("Number of columns changed from " + from + " to " + to + " at line " + line);
	}
}
