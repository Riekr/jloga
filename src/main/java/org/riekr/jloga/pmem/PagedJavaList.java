package org.riekr.jloga.pmem;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

public class PagedJavaList<T> implements List<T> {

	public static PagedJavaList<String> ofStrings() {
		return new PagedJavaList<>(DataEncoder.STRING, DataDecoder.STRING);
	}

	private final @NotNull DataEncoder<T> _encoder;
	private final @NotNull DataDecoder<T> _decoder;

	private PagedList<T> _plist;

	public PagedJavaList(@NotNull DataEncoder<T> encoder, @NotNull DataDecoder<T> decoder) {
		_encoder = encoder;
		_decoder = decoder;
	}

	@Override
	public int size() {
		return _plist == null ? 0 : Math.toIntExact(_plist.size());
	}

	@Override
	public boolean isEmpty() {
		return _plist == null || _plist.size() == 0L;
	}

	@Override
	public boolean contains(Object o) {
		if (o == null)
			return false;
		for (T t : this) {
			if (Objects.equals(o, t))
				return true;
		}
		return false;
	}

	@Override
	public @NotNull Iterator<T> iterator() {
		return new Iterator<>() {
			private long _pos;

			@Override
			public boolean hasNext() {
				return _plist != null && _pos < _plist.size();
			}

			@Override
			public T next() {
				return _plist.get(_pos++);
			}
		};
	}

	@Override
	public Object @NotNull [] toArray() {
		final Object[] res = new Object[size()];
		final Iterator<T> it = iterator();
		for (int i = 0; i < res.length; i++)
			res[i] = it.next();
		return res;
	}

	@SuppressWarnings("unchecked") @Override
	public @NotNull <T1> T1 @NotNull [] toArray(T1 @NotNull [] a) {
		final T1[] res = (T1[])Array.newInstance(a.getClass().getComponentType(), size());
		final Iterator<T> it = iterator();
		for (int i = 0; i < res.length; i++)
			res[i] = (T1)it.next();
		return res;
	}

	@Override
	public boolean add(T t) {
		if (t == null)
			throw new IllegalArgumentException();
		if (_plist == null)
			_plist = new PagedList<>(_encoder, _decoder);
		_plist.add(t);
		return true;
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		return false;
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends T> c) {
		Iterator<? extends T> i = c.iterator();
		if (i.hasNext()) {
			do {
				add(i.next());
			} while (i.hasNext());
			return true;
		}
		return false;
	}

	@Override
	public boolean addAll(int index, @NotNull Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		if (_plist != null) {
			_plist.close();
			_plist = null;
		}
	}

	@Override
	public T get(int index) {
		if (_plist != null) {
			final T res = _plist.get(index);
			if (res != null)
				return res;
		}
		throw new IndexOutOfBoundsException();
	}

	@Override
	public T set(int index, T element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int index, T element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int indexOf(Object o) {
		if (o == null || _plist == null)
			return -1;
		for (int i = 0, l = size(); i < l; i++) {
			if (Objects.equals(o, _plist.get(i)))
				return i;
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		if (o == null || _plist == null)
			return -1;
		for (int i = size() - 1; i >= 0; i--) {
			if (Objects.equals(o, _plist.get(i)))
				return i;
		}
		return -1;
	}

	@Override
	public @NotNull ListIterator<T> listIterator() {
		return listIterator(0);
	}

	@Override
	public @NotNull ListIterator<T> listIterator(int index) {
		return new ListIterator<T>() {
			private long _pos = index;

			@Override
			public boolean hasNext() {
				return _plist != null && _pos < _plist.size();
			}

			@Override
			public T next() {
				return _plist.get(_pos++);
			}

			@Override
			public boolean hasPrevious() {
				return _plist != null && _pos >= 0;
			}

			@Override
			public T previous() {
				return _plist.get(_pos--);
			}

			@Override
			public int nextIndex() {
				return Math.toIntExact(_pos + 1);
			}

			@Override
			public int previousIndex() {
				return Math.toIntExact(_pos - 1);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void set(T t) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void add(T t) {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public @NotNull List<T> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}
}
