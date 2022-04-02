package org.riekr.jloga.project;

import static java.util.Objects.requireNonNullElse;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.riekr.jloga.utils.UIUtils.toDateTimeFormatter;
import static org.riekr.jloga.utils.UIUtils.toPattern;

import java.awt.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.riekr.jloga.misc.DateTimeFormatterRef;
import org.riekr.jloga.ui.MRUComboWithLabels;
import org.riekr.jloga.utils.UIUtils;

public interface Project {

	class Field<T> implements Supplier<T>, Consumer<String> {

		public final String                           key;
		public final String                           label;
		public final BiFunction<String, Component, T> mapper;
		public final Function<T, String>              encode;
		public final String                           deflt;

		private MRUComboWithLabels<?> _ui;
		private String                _src;
		private T                     _value;

		public Field(String key, String label, BiFunction<String, Component, T> mapper, Function<T, String> encode) {
			this(key, label, mapper, encode, null);
		}

		public Field(String key, String label, BiFunction<String, Component, T> mapper, Function<T, String> encode, T deflt) {
			this.key = key;
			this.label = label;
			this.mapper = mapper;
			this.encode = encode;
			this.deflt = deflt == null ? null : encode.apply(deflt);
		}

		@Override
		public T get() {
			return _value == null ? mapper.apply(deflt, _ui) : _value;
		}

		public void set(T value) {
			_src = value == null ? null : encode.apply(value);
			_value = _src == null ? null : value;
			_ui.combo.setValue(_src);
		}

		@Override
		public void accept(String s) {
			_value = mapper.apply(s, _ui);
			_src = _value == null ? null : s;
		}

		public boolean hasValue() {
			return get() != null;
		}

		public String getDescription() {
			return label.trim() + ' ' + requireNonNullElse(_src == null ? deflt : _src, "-");
		}

		public void ui(MRUComboWithLabels<?> ui) {_ui = ui;}

		@Override
		public String toString() {
			return _src;
		}
	}

	default Field<String> newStringField(String key, String label) {
		return new Field<>(key, label, (text, ui) -> text, identity());
	}

	default Field<Pattern> newPatternField(String key, String label) {
		return newPatternField(key, label, 0);
	}

	default Field<Pattern> newPatternField(String key, String label, int minGroups) {
		return new Field<>(key, label, (pattern, ui) -> toPattern(ui, pattern, minGroups), Pattern::pattern);
	}

	default Field<Duration> newDurationField(String key, String label) {
		return new Field<>(key, label, (pattern, ui) -> UIUtils.toDuration(ui, pattern), Duration::toString);
	}

	default Field<Duration> newDurationField(String key, String label, Duration deflt) {
		return new Field<>(key, label, (pattern, ui) -> UIUtils.toDuration(ui, pattern), Duration::toString, deflt);
	}

	default Field<DateTimeFormatterRef> newDateTimeFormatterField(String key, String label) {
		return new Field<>(key, label, (pattern, ui) -> toDateTimeFormatter(ui, pattern), DateTimeFormatterRef::pattern);
	}

	default Field<String> newSelectField(String key, String label, List<String> values) {
		return newSelectField(key, label, values.stream().collect(toMap(identity(), identity())));
	}

	default Field<String> newSelectField(String key, String label, Map<String, String> values) {
		return new Field<>(key, label,
				(text, ui) -> values.get(text),
				(text) -> values.entrySet().stream().filter((e) -> e.getValue().equals(text)).findAny().map(Map.Entry::getKey).orElse(null)) {
			@Override
			public void ui(MRUComboWithLabels<?> ui) {
				// TODO: this method needs to be redesigned!
				ui.combo.removeAllItems();
				values.keySet().forEach(ui.combo::addItem);
				ui.combo.setEditable(false);
				ui.combo.resend();
				super.ui(ui);
			}

			@Override
			public String toString() {
				return get();
			}
		};
	}

	boolean isReady();

	default Stream<? extends Field<?>> fields() {
		return Arrays.stream(getClass().getFields())
				.filter((f) -> Field.class.isAssignableFrom(f.getType()))
				.map((f) -> {
					try {
						return (Field<?>)f.get(this);
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
