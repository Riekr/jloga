package org.riekr.jloga.misc;

import org.riekr.jloga.search.SearchPredicate;
import org.riekr.jloga.ui.UIUtils;

import javax.swing.*;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public abstract class Project implements Supplier<SearchPredicate> {

	public static class Field<T> implements Supplier<T>, Consumer<String> {

		public final String key;
		public final String label;
		public final Function<String, T> mapper;

		private String _src;
		private T _value;

		public Field(String key, String label, Function<String, T> mapper) {
			this.key = key;
			this.label = label;
			this.mapper = mapper;
		}

		@Override
		public T get() {
			return _value;
		}

		public boolean hasValue() {
			return _value != null;
		}

		@Override
		public void accept(String s) {
			_value = mapper.apply(s);
			_src = s;
		}

		@Override
		public String toString() {
			return _src;
		}
	}

	public class PatternField extends Field<Pattern> {
		public PatternField(String key, String label) {
			this(key, label, 0);
		}

		public PatternField(String key, String label, int minGroups) {
			super(key, label, (pattern) -> UIUtils.toPattern(_owner, pattern, minGroups));
		}
	}

	public class DurationField extends Field<Duration> {
		public DurationField(String key, String label) {
			super(key, label, (duration) -> UIUtils.toDuration(_owner, duration));
		}
	}

	public class DateTimeFormatterField extends Field<DateTimeFormatter> {
		public DateTimeFormatterField(String key, String label) {
			super(key, label, (format) -> UIUtils.toDateTimeFormatter(_owner, format));
		}
	}

	protected final JComponent _owner;

	protected Project(JComponent owner) {
		_owner = owner;
	}

	protected JComponent getOwner() {
		return _owner;
	}

	public abstract boolean isReady();

	public Stream<? extends Field<?>> fields() {
		return Arrays.stream(getClass().getFields())
				.filter((f) -> Field.class.isAssignableFrom(f.getType()))
				.map((f) -> {
					try {
						return (Field<?>) f.get(this);
					} catch (IllegalAccessException e) {
						e.printStackTrace(System.err);
						return null;
					}
				})
				.filter(Objects::nonNull);
	}

}
