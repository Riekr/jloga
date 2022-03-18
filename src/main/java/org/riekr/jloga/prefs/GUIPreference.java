package org.riekr.jloga.prefs;

import static java.util.stream.Stream.concat;
import static org.riekr.jloga.ui.utils.TextUtils.escapeHTML;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.react.Observer;
import org.riekr.jloga.react.Unsubscribable;

public class GUIPreference<T> implements Preference<T> {

	public enum Type {
		Font, Combo, Toggle, Directory
	}

	private final Preference<T> _pref;
	private final Type          _type;
	private final String        _title;
	private       String[]      _descriptions;
	private       String        _group;

	public GUIPreference(Preference<T> pref, Type type, String title) {
		_pref = pref;
		_type = type;
		_title = title;
	}

	private List<Consumer<Map<String, T>>> _values;

	public Type type() {return _type;}

	public String title() {return _title;}

	public String description() {
		if (_descriptions == null)
			return null;
		return "<html>" + String.join("<br>", _descriptions) + "</html>";
	}

	public String group() {
		return _group;
	}

	public GUIPreference<T> group(String group) {
		if (_group != null)
			throw new IllegalStateException("Group already specified");
		Objects.requireNonNull(group, "Group must be specified");
		_group = group;
		return this;
	}

	public GUIPreference<T> addDescription(String description) {
		if (description != null && !description.isEmpty()) {
			description = escapeHTML(description);
			if (_descriptions == null)
				_descriptions = new String[]{description};
			else
				_descriptions = concat(Stream.of(_descriptions), Stream.of(description)).toArray(String[]::new);
		}
		return this;
	}

	public Set<Map.Entry<String, T>> values() {
		if (_values == null || _values.isEmpty())
			return Collections.emptySet();
		Map<String, T> values = new TreeMap<>();
		_values.forEach((filler) -> filler.accept(values));
		return values.entrySet();
	}

	public GUIPreference<T> add(Supplier<Map<String, T>> values) {
		if (_values == null)
			_values = new ArrayList<>();
		_values.add((res) -> res.putAll(values.get()));
		return this;
	}

	public GUIPreference<T> add(Function<T, String> descriptor, Supplier<T[]> values) {
		if (_values == null)
			_values = new ArrayList<>();
		_values.add((res) -> {
			for (T t : values.get())
				res.put(descriptor.apply(t), t);
		});
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

	@Override
	public T get() {return _pref.get();}

	@Override
	public boolean set(T t) {return _pref.set(t);}

	@Override
	public T reset() {return _pref.reset();}

	@Override
	public GUIPreference<T> describe(Type type, String title) {
		return new GUIPreference<>(_pref, type, title);
	}

	@Override
	public @NotNull Unsubscribable subscribe(Observer<? super T> observer) {
		return _pref.subscribe(observer);
	}
}
