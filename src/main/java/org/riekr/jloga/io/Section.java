package org.riekr.jloga.io;

public record Section(
		String name,
		int from,
		int to
) {

	@Override
	public String toString() {
		return name;
	}

}
