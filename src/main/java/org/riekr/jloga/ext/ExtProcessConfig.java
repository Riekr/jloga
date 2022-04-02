package org.riekr.jloga.ext;

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.riekr.jloga.project.ProjectField;
import org.riekr.jloga.search.SearchComponent;

public class ExtProcessConfig {

	public enum ParamType {
		STRING, PATTERN, DURATION, COMBO
	}

	public static class Param {
		public String    description;
		public ParamType type;
		public boolean   mandatory;
		public int       min;
		public String    deflt;
		public Object    values;

		public transient ProjectField<?, ?> _field;
	}

	public String             workingDirectory;
	public String             icon;
	public String             label;
	public String             description;
	public Object[]           command;
	public int                order;
	public Map<String, Param> params;

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

	public SearchComponent toComponent(String id, int level) {
		if (params == null || params.isEmpty())
			return new ExtProcessComponent(id, icon, label, new File(workingDirectory), getCommand());
		return new ExtProcessComponentProject(id, icon, level, new File(workingDirectory), getCommand(), params);
	}
}
