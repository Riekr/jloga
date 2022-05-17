package org.riekr.jloga.ext;

import static java.util.stream.Collectors.joining;
import static org.riekr.jloga.utils.PopupUtils.popupError;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.riekr.jloga.project.ProjectField;
import org.riekr.jloga.search.SearchComponent;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings("CanBeFinal")
public class ExtProcessConfig {

	public enum ParamType {
		STRING, PATTERN, DURATION, COMBO, CHECKBOX
	}

	public static class Param {
		public                            String    description;
		public                            ParamType type;
		public                            boolean   mandatory;
		public                            int       min;
		@SerializedName("default") public String    deflt;
		public                            Object    values;

		public transient ProjectField<?, ?> _field;
	}

	public String             workingDirectory;
	public String             icon;
	public String             label;
	public String             description;
	public Object[]           command;
	public int                order;
	public Map<String, Param> params;
	public String             matchRegex;
	public boolean            enabled = true;

	public transient String _id;

	public List<String> getCommand() {
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
			throw popupError(new IllegalArgumentException("No command specified in '" + label + '\''));
		res.trimToSize();
		return res;
	}

	public Pattern getMatchRegex() {
		if (matchRegex != null && !matchRegex.isBlank()) {
			if (matchRegex.equalsIgnoreCase("grep"))
				return Pattern.compile("^(?<file>[^:]*):(?<line>\\d*):(?<text>.*)");
			try {
				return Pattern.compile(matchRegex);
			} catch (PatternSyntaxException err) {
				throw popupError("Invalid 'matchRegex' in '" + label + '\'', err);
			}
		}
		return null;
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
			return new ExtProcessComponent(id, icon, label, new File(workingDirectory), getCommand(), getMatchRegex());
		return new ExtProcessComponentProject(id, icon, level, new File(workingDirectory), getCommand(), params, getMatchRegex());
	}
}
