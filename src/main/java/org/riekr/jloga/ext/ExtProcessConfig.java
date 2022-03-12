package org.riekr.jloga.ext;

import java.util.Arrays;

public class ExtProcessConfig {

	public final String   icon;
	public final String   label;
	public final String   description;
	public final String[] command;

	public ExtProcessConfig() {
		icon = null;
		label = null;
		description = null;
		command = null;
	}

	@Override public String toString() {
		return "ExtProcessConfig{" +
				"icon='" + icon + '\'' +
				", label='" + label + '\'' +
				", description='" + description + '\'' +
				", command=" + Arrays.toString(command) +
				'}';
	}
}
