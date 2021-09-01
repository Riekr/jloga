package org.riekr.jloga.misc;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public class Formatters {

	public static DateTimeFormatter newDefaultDateTimeFormatter(String pattern) {
		return new DateTimeFormatterBuilder()
				.appendPattern(pattern)
				.toFormatter()
				.withLocale(Locale.ENGLISH)
				.withZone(ZoneId.systemDefault());
	}

	private Formatters() {}
}
