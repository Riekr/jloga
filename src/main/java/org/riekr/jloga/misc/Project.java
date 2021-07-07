package org.riekr.jloga.misc;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.ui.UIUtils;

import java.awt.*;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.joining;

public interface Project {

	class Field<T> implements Supplier<T>, Consumer<String> {

		public final String key;
		public final String label;
		public final BiFunction<String, Component, T> mapper;
		public final String deflt;

		public Component ui;

		private String _src;
		private T _value;

		public Field(String key, String label, BiFunction<String, Component, T> mapper) {
			this(key, label, mapper, null);
		}

		public Field(String key, String label, BiFunction<String, Component, T> mapper, String deflt) {
			this.key = key;
			this.label = label;
			this.mapper = mapper;
			this.deflt = deflt;
		}

		@Override
		public T get() {
			return _value == null ? mapper.apply(deflt, ui) : _value;
		}

		public boolean hasValue() {
			return get() != null;
		}

		@Override
		public void accept(String s) {
			_value = mapper.apply(s, ui);
			_src = _value == null ? null : s;
		}

		public String getDescription() {
			return label.trim() + ' ' + requireNonNullElse(_src == null ? deflt : _src, "-");
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
		return new Field<>(key, label, (pattern, ui) -> UIUtils.toPattern(ui, pattern, minGroups));
	}

	default Field<Duration> newDurationField(String key, String label) {
		return new Field<>(key, label, (pattern, ui) -> UIUtils.toDuration(ui, pattern));
	}

	default Field<Duration> newDurationField(String key, String label, @NotNull Duration deflt) {
		return new Field<>(key, label, (pattern, ui) -> UIUtils.toDuration(ui, pattern), deflt.toString());
	}

	default Field<DateTimeFormatter> newDateTimeFormatterField(String key, String label) {
		return new Field<>(key, label, (pattern, ui) -> UIUtils.toDateTimeFormatter(ui, pattern));
	}

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
