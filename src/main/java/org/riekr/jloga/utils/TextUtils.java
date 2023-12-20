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

	public static String humanReadableByteCountSI(long bytes) {
		return humanReadableByteCount(bytes, true);
	}

	public static String humanReadableByteCount(long bytes, boolean si) {
		// yes, it is! -> https://programming.guide/worlds-most-copied-so-snippet.html
		int unit = si ? 1000 : 1024;
		long absBytes = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
		if (absBytes < unit)
			return bytes + " B";
		int exp = (int)(Math.log(absBytes) / Math.log(unit));
		long th = (long)Math.ceil(Math.pow(unit, exp) * (unit - 0.05));
		if (exp < 6 && absBytes >= th - ((th & 0xFFF) == 0xD00 ? 51 : 0))
			exp++;
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		if (exp > 4) {
			bytes /= unit;
			exp -= 1;
		}
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	public static String toString(@NotNull String[] strings, int count) {
		if (strings.length < count)
			count = strings.length;
		switch (count) {
			case 0:
				return "";
			case 1:
				return strings[0];
			default:
				if (count < 0)
					throw new IllegalArgumentException();
		}
		StringBuilder buf = new StringBuilder(160 * count);
		buf.append(strings[0]);
		for (int i = 1; i < count; i++)
			buf.append('\n').append(strings[i]);
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
		int i = from + 1;
		for (; i < lim; i++)
			buf.append('\n').append(strings[i]);
		len = from + count;
		for (; i < len; i++)
			buf.append('\n');
		return buf.toString();
	}

	public static String ellipsize(String text, int limit) {
		if (text == null || text.isEmpty())
			return "";
		if (text.length() > limit)
			text = text.substring(0, limit) + "\u2026";
		return text;
	}

	private TextUtils() {}
}
