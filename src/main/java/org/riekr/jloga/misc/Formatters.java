package org.riekr.jloga.misc;

import org.riekr.jloga.prefs.Preferences;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public class Formatters {

	public static DateTimeFormatter newDefaultDateTimeFormatter(String pattern) {
		return new DateTimeFormatterBuilder()
				.appendPattern(pattern)
				.toFormatter()
				.withLocale(Preferences.LOCALE.get())
				.withZone(ZoneId.systemDefault());
	}

	private Formatters() {}
}
