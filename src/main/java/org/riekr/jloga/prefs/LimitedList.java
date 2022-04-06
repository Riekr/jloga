package org.riekr.jloga.prefs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

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
}
