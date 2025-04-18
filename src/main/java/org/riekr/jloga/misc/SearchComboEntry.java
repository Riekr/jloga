package org.riekr.jloga.misc;

import java.io.Serial;
import java.io.Serializable;

public class SearchComboEntry implements Serializable {
	@Serial private static final long serialVersionUID = -6828265343399858457L;

	public final String pattern;

	public boolean negate;
	public boolean caseInsensitive;

	public SearchComboEntry(String pattern) {
		this.pattern = pattern;
	}

	@Override
	public String toString() {
		return pattern;
	}
}
