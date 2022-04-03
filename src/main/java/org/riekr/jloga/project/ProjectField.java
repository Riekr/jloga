package org.riekr.jloga.project;

import static java.util.Objects.requireNonNullElse;

import java.awt.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public abstract class ProjectField<T, UIType extends Component> implements Supplier<T>, Consumer<String> {

	public final String                           key;
	public final String                           label;
	public final BiFunction<String, Component, T> mapper;
	public final Function<T, String>              encode;
	public final String                           deflt;

	protected UIType _ui;
	protected String _src;
	protected T      _value;

	public ProjectField(String key, String label, BiFunction<String, Component, T> mapper, Function<T, String> encode) {
		this(key, label, mapper, encode, null);
	}

	public ProjectField(String key, String label, BiFunction<String, Component, T> mapper, Function<T, String> encode, T deflt) {
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

	@NotNull
	public UIType ui(ProjectComponent panel) {
		if (_ui == null)
			_ui = newUI(panel);
		return _ui;
	}

	@Contract("_->new")
	protected abstract UIType newUI(ProjectComponent panel);

	@Override
	public String toString() {
		return _src;
	}
}
