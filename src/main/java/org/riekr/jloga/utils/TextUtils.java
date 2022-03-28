package org.riekr.jloga.utils;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import javax.swing.*;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.prefs.GUIPreference;

public class TextUtils {

	public static final String TAB_ADD = " + ";

	// https://stackoverflow.com/a/25228492/1326326
	public static String escapeHTML(String s) {
		StringBuilder out = new StringBuilder(Math.max(16, s.length()));
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c > 127 || c == '"' || c == '\'' || c == '<' || c == '>' || c == '&') {
				out.append("&#");
				out.append((int)c);
				out.append(';');
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}

	public static String replaceRegex(String orig, Pattern pattern, Map<String, String> env) {
		Matcher matcher = pattern.matcher(orig);
		if (matcher.find()) {
			StringBuilder buf = new StringBuilder();
			do {
				String key = matcher.group(1);
				String val = env.get(key);
				if (val == null)
					throw new IllegalArgumentException("Unbound variable name: " + key);
				matcher.appendReplacement(buf, val);
			} while (matcher.find());
			matcher.appendTail(buf);
			return buf.toString();
		}
		return orig;
	}

	public static String describeKeyBinding(@NotNull GUIPreference<KeyStroke> key) {
		return describeKeyBinding(key.get(), key.title());
	}

	public static String describeKeyBinding(@NotNull KeyStroke key, @Nullable String description) {
		String res = stream(key.toString().toUpperCase().split(" +"))
				.filter((s -> !s.equals("PRESSED")))
				.map(s -> s.equals("CONTROL") ? "CTRL" : s)
				// .peek(System.out::println)
				.distinct().collect(joining("+"));
		// System.out.println(res);
		res = "<b>" + res.toUpperCase() + "</b>";
		if (description != null && !(description = description.trim()).isEmpty())
			res += "&nbsp;=&nbsp;" + description.replace(" ", "&nbsp;");
		return res;
	}

	private TextUtils() {}
}
