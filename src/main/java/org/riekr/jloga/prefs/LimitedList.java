package org.riekr.jloga.prefs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

public class LimitedList<T extends Serializable> extends ArrayList<T> {
	private static final long serialVersionUID = -2914897583298816672L;

	private final int _size;

	public LimitedList(int size) {
		super(size);
		_size = size;
	}

	public LimitedList(LimitedList<T> orig) {
		_size = orig._size;
		super.addAll(orig);
	}

	public void roll(T t) {
		remove(t);
		add(0, t);
	}

	public <R> R peekFirst(Function<T, R> conv) {
		return isEmpty() ? null : conv.apply(get(0));
	}

	public T peekFirst() {
		return isEmpty() ? null : get(0);
	}

	@Override
	public boolean add(T t) {
		try {
			return super.add(t);
		} finally {
			if (super.size() > _size)
				remove(0);
		}
	}

	@Override
	public void add(int index, T element) {
		if (index >= _size)
			throw new IndexOutOfBoundsException("This list is limited to " + _size + " elements, can't add at" + index);
		try {
			super.add(index, element);
		} finally {
			if (super.size() > _size)
				remove(_size);
		}
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		try {
			return super.addAll(c);
		} finally {
			int sz = super.size();
			if (sz > _size)
				removeRange(0, sz - _size);
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		if (index >= _size)
			throw new IndexOutOfBoundsException("This list is limited to " + _size + " elements, can't add at" + index);
		try {
			return super.addAll(index, c);
		} finally {
			int sz = super.size();
			if (sz > _size)
				removeRange(0, sz - _size);
		}
	}

	public LimitedList<T> nonNulls() {
		LimitedList<T> res = new LimitedList<>(_size);
		stream().filter(Objects::nonNull).forEach(res::add);
		return res;
	}
}
