package org.riekr.jloga.misc;

import org.riekr.jloga.ui.UIUtils;

import java.awt.*;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public interface Project {

	class Field<T> implements Supplier<T>, Consumer<String> {

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

		public String getDescription() {
			return label.trim() + ' ' + _src;
		}

		@Override
		public String toString() {
			return _src;
		}
	}

	default Field<Pattern> newPatternField(String key, String label) {
		return newPatternField(key, label, 0);
	}

	default Field<Pattern> newPatternField(String key, String label, int minGroups) {
		return new Field<>(key, label, (pattern) -> UIUtils.toPattern(getDialogParentComponent(), pattern, minGroups));
	}

	default Field<Duration> newDurationField(String key, String label) {
		return new Field<>(key, label, (pattern) -> UIUtils.toDuration(getDialogParentComponent(), pattern));
	}

	default Field<DateTimeFormatter> newDateTimeFormatterField(String key, String label) {
		return new Field<>(key, label, (pattern) -> UIUtils.toDateTimeFormatter(getDialogParentComponent(), pattern));
	}

	Component getDialogParentComponent();

	boolean isReady();

	default Stream<? extends Field<?>> fields() {
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

	default void clear() {
		fields().forEach((f) -> f.accept(null));
	}

	default String getDescription() {
		return fields()
				.map(Field::getDescription)
				.collect(joining(" | "));
	}

}
