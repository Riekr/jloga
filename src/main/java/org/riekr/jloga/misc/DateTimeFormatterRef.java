package org.riekr.jloga.misc;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.prefs.Preferences;

public class DateTimeFormatterRef {

	// http://mail.openjdk.java.net/pipermail/threeten-dev/2015-March/001611.html
	public static DateTimeFormatterRef ofPattern(String pattern) {
		return new DateTimeFormatterRef(pattern, new DateTimeFormatterBuilder()
				.appendPattern(pattern)
				.toFormatter()
				.withLocale(Preferences.LOCALE.get())
				.withZone(ZoneId.systemDefault())
		);
	}

	public final          String            pattern;
	public final @NotNull DateTimeFormatter formatter;

	private DateTimeFormatterRef(@NotNull String pattern, @NotNull DateTimeFormatter formatter) {
		this.pattern = pattern;
		this.formatter = formatter;
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		DateTimeFormatterRef that = (DateTimeFormatterRef)o;
		return formatter.equals(that.formatter);
	}

	@Override
	public int hashCode() {
		return formatter.hashCode();
	}

	@Override
	public String toString() {
		return pattern;
	}

	public String pattern() {
		return pattern;
	}

}
