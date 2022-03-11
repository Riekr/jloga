package org.riekr.jloga.pmem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PagedList<T extends Serializable> implements Closeable {

	@SuppressWarnings("unchecked")
	static class Page<T> implements Supplier<ArrayList<T>>, Consumer<ArrayList<T>> {
		private final     File                        _file;
		private @Nullable WeakReference<ArrayList<T>> _cache;
		final             int                         id;

		Page(File file, ArrayList<T> data, int id) {
			_file = file;
			this.id = id;
			accept(data);
		}

		@Override
		public ArrayList<T> get() {
			ArrayList<T> res;
			String from = "memory";
			if (_cache == null || (res = _cache.get()) == null) {
				from = "disk";
				try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(_file)))) {
					res = (ArrayList<T>)ois.readObject();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
				_cache = new WeakReference<>(res);
			}
			System.out.println("Loaded page " + id + " from " + from);
			return res;
		}

		@Override
		public void accept(ArrayList<T> data) {
			data.trimToSize();
			try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(_file)))) {
				oos.writeObject(data);
				_cache = new WeakReference<>(data);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	static class Live<T> {
		final ArrayList<T> data;
		final int          start;
		Page<T> page;

		Live(ArrayList<T> data, int start) {
			this.data = data;
			this.start = start;
		}

		Live(@NotNull Page<T> page, int start) {
			this.page = page;
			this.data = page.get();
			this.start = start;
		}
	}

	private final int _limit;

	private final @NotNull TreeMap<Integer, Page<T>> _pages = new TreeMap<>();
	private @Nullable      Live<T>                   _writing;
	private @NotNull       Live<T>                   _reading;
	private                long                      _size  = 0;

	public PagedList(int pageSizeLimit) {
		if (pageSizeLimit <= 0)
			throw new IllegalArgumentException("Invalid page limit size: " + pageSizeLimit);
		_limit = pageSizeLimit;
		_writing = new Live<>(new ArrayList<>(_limit), 0);
		_reading = _writing;
	}

	private File createTempFile() {
		try {
			return File.createTempFile("jloga", Integer.toHexString(PagedList.this.hashCode()));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void seal() {
		if (_writing != null) {
			if (!_writing.data.isEmpty())
				save();
			_writing = null;
		}
	}

	public boolean isSealed() {
		return _writing == null;
	}

	private void save() {
		if (_writing == null)
			throw new IllegalStateException("PagedIntBag is sealed");
		_writing.data.trimToSize();
		if (_writing.page != null || _pages.put(_writing.start, _writing.page = new Page<>(createTempFile(), _writing.data, _pages.size() + 1)) != null)
			throw new IllegalStateException("Page " + _writing.start + " already saved");
		_writing = new Live<>(new ArrayList<>(_limit), _writing.start + _writing.data.size());
	}

	public final void add(T value) {
		if (_writing == null)
			throw new IllegalStateException("PagedIntBag is sealed");
		_writing.data.add(value);
		_size++;
		if (_writing.data.size() == _limit)
			save();
	}

	public T get(int index) {
		int idx = index - _reading.start;
		if (idx < 0 || idx >= _reading.data.size()) {
			Map.Entry<Integer, Page<T>> e = _pages.floorEntry(index);
			int newStart = e.getKey();
			idx = index - newStart;
			if (idx < _reading.data.size()) {
				_reading = new Live<>(e.getValue(), newStart);
			} else {
				if (_writing == null)
					throw new IndexOutOfBoundsException("Requested index " + index + " while size is " + _size);
				_reading = _writing;
				idx = index - _reading.start;
			}
		}
		return _reading.data.get(idx);
	}

	@Override
	public void close() {
		_pages.values().forEach((p) -> {
			if (!p._file.delete())
				System.err.println("Unable to delete paging file " + p._file);
		});
		_writing = null;
		_pages.clear();
	}

	public long size() {
		return _size;
	}

	public int pages() {
		return _pages.size();
	}

}
