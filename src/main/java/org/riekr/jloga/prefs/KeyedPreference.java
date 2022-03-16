package org.riekr.jloga.prefs;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.react.Observer;
import org.riekr.jloga.react.Subject;
import org.riekr.jloga.react.Unsubscribable;

public class KeyedPreference<T extends Serializable> implements Preference<T> {

	private final String      _key;
	private final Supplier<T> _deflt;
	private final Subject<T>  _subject = new Subject<>();

	private boolean _valid;
	private T       _value;

	protected KeyedPreference(String key, Supplier<T> deflt) {
		_key = key;
		_deflt = deflt;
	}

	@Override
	public @NotNull Unsubscribable subscribe(Observer<? super T> observer) {
		observer.onNext(get());
		return _subject.subscribe(observer);
	}

	private String key(Object[] path) {
		StringBuilder res = new StringBuilder(_key);
		for (Object o : path)
			res.append('.').append(o);
		return res.toString();
	}

	@Override
	public boolean set(T t) {
		if (!Objects.equals(t, _value)) {
			PrefsUtils.save(_key, t);
			_value = t;
			_valid = true;
			_subject.next(t);
			return true;
		}
		return false;
	}

	@Override
	public T reset() {
		T res = _deflt == null ? null : _deflt.get();
		set(res);
		return res;
	}

	@Override
	public T get() {
		if (!_valid) {
			_value = PrefsUtils.load(_key, _deflt);
			_valid = true;
		}
		return _value;
	}

	public T get(Object... path) {
		if (path == null || path.length == 0)
			return get();
		return PrefsUtils.load(key(path), _deflt);
	}

	public void set(T t, Object... path) {
		if (path == null || path.length == 0) {
			set(t);
			return;
		}
		PrefsUtils.save(key(path), t);
	}

}
