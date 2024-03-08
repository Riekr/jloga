package org.riekr.jloga.prefs;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.riekr.jloga.utils.TextUtils.escapeHTML;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.react.BoolBehaviourSubject;
import org.riekr.jloga.react.Observer;
import org.riekr.jloga.react.Unsubscribable;

public class GUIPreference<T> implements Preference<T> {

	public enum Type {
		Font, Combo, Toggle, Executable, Directory, KeyBinding, FileMap
	}

	public final BoolBehaviourSubject enabled = new BoolBehaviourSubject(true);

	private final Preference<T> _pref;
	private final Type          _type;
	private final String        _title;
	private       List<String>  _descriptions;
	private       String        _group;

	public GUIPreference(Preference<T> pref, Type type, String title) {
		_pref = pref;
		_type = type;
		_title = title;
	}

	private Consumer<Map<String, T>> _values;

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
		if (description != null && !description.isEmpty())
			addDescriptionHTML(escapeHTML(description));
		return this;
	}

	public GUIPreference<T> addDescriptionHTML(String description) {
		if (description != null && !description.isEmpty()) {
			if (_descriptions == null)
				_descriptions = new ArrayList<>(1);
			_descriptions.add(description);
		}
		return this;
	}

	public Set<Map.Entry<String, T>> values() {
		if (_values == null)
			return emptySet();
		Map<String, T> values = new LinkedHashMap<>();
		_values.accept(values);
		return values.entrySet();
	}

	@Nullable
	public T nextOf(T curr) {
		List<T> values = values().stream()
				.map(Map.Entry::getValue)
				.collect(toList());
		if (values.isEmpty())
			return null;
		Iterator<T> i = values.iterator();
		while (i.hasNext()) {
			T t = i.next();
			if (Objects.equals(t, curr) && i.hasNext())
				return i.next();
		}
		return values.get(0);
	}

	public GUIPreference<T> add(Consumer<Map<String, T>> filler) {
		_values = _values == null ? filler : _values.andThen(filler);
		return this;
	}

	public GUIPreference<T> add(Supplier<Map<String, T>> values) {
		return add((res) -> res.putAll(values.get()));
	}

	public GUIPreference<T> add(Function<T, String> descriptor, Supplier<T[]> values) {
		return add((res) -> {
			for (T t : values.get())
				res.put(descriptor.apply(t), t);
		});
	}

	public GUIPreference<T> add(String description, T value) {
		return add(description, () -> value);
	}

	public GUIPreference<T> add(String description, Supplier<T> value) {
		return add((res) -> res.put(description, value.get()));
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

	public <V> GUIPreference<T> require(GUIPreference<V> otherPref, V val) {
		otherPref.subscribe((otherVal) -> enabled.next(Objects.equals(val, otherVal)));
		return this;
	}

	public boolean available() {
		return true;
	}
}
