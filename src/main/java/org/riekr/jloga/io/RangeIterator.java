package org.riekr.jloga.io;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.IntFunction;

public final class RangeIterator<T> implements Iterator<T> {

	private final int            _toExclusive;
	private       int            _index;
	private final IntFunction<T> _getter;

	@SafeVarargs
	public RangeIterator(T... data) {
		this(0, data.length, data);
	}

	@SafeVarargs
	public RangeIterator(int fromInclusive, int toExclusive, T... data) {
		this(fromInclusive, Math.min(toExclusive, data.length), (i) -> data[i]);
	}

	public RangeIterator(int fromInclusive, int toExclusive, IntFunction<T> getter) {
		_index = fromInclusive;
		_toExclusive = toExclusive;
		_getter = getter;
	}

	@Override
	public boolean hasNext() {
		return _index < _toExclusive;
	}

	@Override
	public T next() {
		if (_index < _toExclusive)
			return _getter.apply(_index++);
		throw new NoSuchElementException();
	}
}
