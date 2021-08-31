package org.riekr.jloga.misc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PagedIntBag implements Closeable {

	@SuppressWarnings("unchecked")
	static class Page implements Supplier<ArrayList<int[]>>, Consumer<ArrayList<int[]>> {
		private final File _file;
		private @Nullable WeakReference<ArrayList<int[]>> _cache;
		final int id;

		Page(File file, ArrayList<int[]> data, int id) {
			_file = file;
			this.id = id;
			accept(data);
		}

		@Override
		public ArrayList<int[]> get() {
			ArrayList<int[]> res;
			String from = "memory";
			if (_cache == null || (res = _cache.get()) == null) {
				from = "disk";
				try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(_file)))) {
					res = (ArrayList<int[]>) ois.readObject();
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
		public void accept(ArrayList<int[]> data) {
			data.trimToSize();
			try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(_file)))) {
				oos.writeObject(data);
				_cache = new WeakReference<>(data);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	static class Live {
		final ArrayList<int[]> data;
		final int start;
		Page page;

		Live(ArrayList<int[]> data, int start) {
			this.data = data;
			this.start = start;
		}

		Live(@NotNull Page page, int start) {
			this.page = page;
			this.data = page.get();
			this.start = start;
		}
	}

	private static final int _MAX_PAGE_SIZE = 2048 * 1024; // 2MB

	private final int _depth;
	private final int _limit;

	private final @NotNull TreeMap<Integer, Page> _pages = new TreeMap<>();
	private @Nullable Live _writing;
	private @NotNull Live _reading;
	private long _size = 0;

	public PagedIntBag(int depth) {
		int max = _MAX_PAGE_SIZE / Integer.BYTES;
		if (depth <= 0 || depth > max)
			throw new IllegalArgumentException("Depth must be between 1 and " + max + " inclusive");
		_depth = depth;
		_limit = _MAX_PAGE_SIZE / (Integer.BYTES * depth);
		_writing = new Live(new ArrayList<>(_limit), 0);
		_reading = _writing;
	}

	private File createTempFile() {
		try {
			return File.createTempFile("jloga", Integer.toHexString(PagedIntBag.this.hashCode()));
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

	private void save() {
		if (_writing == null)
			throw new IllegalStateException("PagedIntBag is sealed");
		_writing.data.trimToSize();
		if (_writing.page != null || _pages.put(_writing.start, _writing.page = new Page(createTempFile(), _writing.data, _pages.size() + 1)) != null)
			throw new IllegalStateException("Page " + _writing.start + " already saved");
		_writing = new Live(new ArrayList<>(_limit), _writing.start + _writing.data.size());
	}

	public void add(int @NotNull ... values) {
		if (_writing == null)
			throw new IllegalStateException("PagedIntBag is sealed");
		if (values.length != _depth)
			throw new IllegalArgumentException("Argument length must be " + _depth);
		_writing.data.add(values);
		_size++;
		if (_writing.data.size() == _limit)
			save();
	}

	public int[] get(int index) {
		int idx = index - _reading.start;
		if (idx < 0 || idx >= _reading.data.size()) {
			Map.Entry<Integer, Page> e = _pages.floorEntry(index);
			int newStart = e.getKey();
			idx = index - newStart;
			if (idx < _reading.data.size()) {
				_reading = new Live(e.getValue(), newStart);
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
