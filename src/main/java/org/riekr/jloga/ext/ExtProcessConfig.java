package org.riekr.jloga.ext;

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class ExtProcessConfig {

	public File     workingDirectory;
	public String   icon;
	public String   label;
	public String   description;
	public Object[] command;
	public int      order;

	public String[] getCommand() {
		ArrayList<String> res = new ArrayList<>();
		for (Object arg : command) {
			if (arg == null)
				continue;
			if (arg instanceof Collection) {
				res.add(((Collection<?>)arg).stream()
						.filter(Objects::nonNull)
						.map(String::valueOf)
						.filter((s) -> !s.isEmpty())
						.collect(joining(File.pathSeparator)));
			} else
				res.add(arg.toString());
		}
		if (res.isEmpty())
			throw new IllegalArgumentException("No command specified in '" + label + '\'');
		return res.toArray(String[]::new);
	}

	@Override public String toString() {
		return "ExtProcessConfig{" +
				"icon='" + icon + '\'' +
				", label='" + label + '\'' +
				", description='" + description + '\'' +
				", command=" + Arrays.toString(command) +
				", order=" + order +
				'}';
	}
}
