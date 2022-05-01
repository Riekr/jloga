package org.riekr.jloga.utils;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import javax.swing.*;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
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

	public static String humanReadableByteCountSI(long bytes) {
		// yes, it is! -> https://programming.guide/worlds-most-copied-so-snippet.html
		if (-1000 < bytes && bytes < 1000)
			return bytes + " bytes";
		//noinspection SpellCheckingInspection
		CharacterIterator ci = new StringCharacterIterator("kMGTPE");
		while (bytes <= -999_950 || bytes >= 999_950) {
			bytes /= 1000;
			ci.next();
		}
		return String.format("%.1f %cB", bytes / 1000.0, ci.current());
	}

	public static String toString(@NotNull String[] strings, int count) {
		switch (count) {
			case 0:
				return "";
			case 1:
				return strings.length != 0 ? strings[0] : "";
			default:
				if (count < 0)
					throw new IllegalArgumentException();
		}
		StringBuilder buf = new StringBuilder(100 * count);
		buf.append(strings[0]);
		int len = Math.min(strings.length, count);
		int i = 1;
		for (; i < len; i++)
			buf.append('\n').append(strings[i]);
		for (; i < count; i++)
			buf.append('\n');
		return buf.toString();
	}

	public static String toString(int from, int len, @NotNull String[] strings, int count) {
		switch (count) {
			case 0:
				return "";
			case 1:
				return strings.length > from ? strings[from] : "";
			default:
				if (count < 0)
					throw new IllegalArgumentException();
		}
		final int lim = Math.min(from + len, strings.length);
		StringBuilder buf = new StringBuilder(100 * count);
		buf.append(strings[from]);
		int i = from;
		for (; i < lim; i++)
			buf.append('\n').append(strings[i]);
		len = from + count;
		for (; i < len; i++)
			buf.append('\n');
		return buf.toString();
	}

	private TextUtils() {}
}
