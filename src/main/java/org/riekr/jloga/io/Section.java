package org.riekr.jloga.io;

public class Section {

	public final String name;
	public final int    from, to;

	public Section(String name, int from, int to) {
		this.name = name;
		this.from = from;
		this.to = to;
	}

	@Override
	public String toString() {
		return name;
	}
}
