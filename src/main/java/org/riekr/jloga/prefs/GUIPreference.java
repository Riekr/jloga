package org.riekr.jloga.prefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class GUIPreference<T> implements Preference<T> {

	private List<Consumer<Map<String, T>>> _values;

	public enum Type {
		Font, Combo, Toggle, Directory
	}

	public abstract Type type();

	public abstract String title();

	public abstract String description();

	public Set<Map.Entry<String, T>> values() {
		if (_values == null || _values.isEmpty())
			return Collections.emptySet();
		Map<String, T> values = new LinkedHashMap<>();
		_values.forEach((filler) -> filler.accept(values));
		return values.entrySet();
	}

	public GUIPreference<T> add(Supplier<Map<String, T>> values) {
		if (_values == null)
			_values = new ArrayList<>();
		_values.add((res) -> res.putAll(values.get()));
		return this;
	}

	public GUIPreference<T> add(String description, T value) {
		return add(description, () -> value);
	}

	public GUIPreference<T> add(String description, Supplier<T> value) {
		if (_values == null)
			_values = new ArrayList<>();
		_values.add((res) -> res.put(description, value.get()));
		return this;
	}

}
