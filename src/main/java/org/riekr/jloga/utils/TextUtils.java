package org.riekr.jloga.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtils {

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

	private TextUtils() {}
}
