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

		Page(File file, ArrayList<int[]> data) {
			_file = file;
			accept(data);
		}

		@Override
		public ArrayList<int[]> get() {
			ArrayList<int[]> res;
			if (_cache == null || (res = _cache.get()) == null) {
				try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(_file)))) {
					res = (ArrayList<int[]>) ois.readObject();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
				_cache = new WeakReference<>(res);
			}
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

	private static final int _MAX_PAGE_SIZE = 2048 * 1024; // 2MB

	private final int _depth;
	private final int _limit;

	private @Nullable TreeMap<Integer, Page> _pages;
	private @NotNull ArrayList<int[]> _page;
	private int _start = 0;
	private long _size = 0;

	public PagedIntBag(int depth) {
		int max = _MAX_PAGE_SIZE / Integer.BYTES;
		if (depth <= 0 || depth > max)
			throw new IllegalArgumentException("Depth must be between 1 and " + max + " inclusive");
		_pages = new TreeMap<>();
		_depth = depth;
		_limit = _MAX_PAGE_SIZE / (Integer.BYTES * depth);
		_page = new ArrayList<>(_limit);
	}

	private File createTempFile() {
		try {
			return File.createTempFile("jloga", Integer.toHexString(PagedIntBag.this.hashCode()));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void save() {
		if (_pages == null)
			throw new IllegalStateException("PagedIntBag is closed");
		if (_pages.put(_start, new Page(createTempFile(), _page)) != null)
			throw new IllegalStateException("Page " + _start + " already saved");
		_start += _page.size();
		_page = new ArrayList<>(_limit);
	}

	public void add(int @NotNull ... values) {
		if (values.length != _depth)
			throw new IllegalArgumentException("Argument length must be " + _depth);
		if (_pages == null)
			throw new IllegalStateException("PagedIntBag is closed");
		_page.add(values);
		_size++;
		if (_page.size() == _limit)
			save();
	}

	public int[] get(int index) {
		if (_pages == null)
			throw new IllegalStateException("PagedIntBag is closed");
		ArrayList<int[]> page = _page;
		int start = _start;
		int idx = index - start;
		if (idx < 0 || idx >= _start + _page.size()) {
			Map.Entry<Integer, Page> e = _pages.floorEntry(index);
			start = e.getKey();
			page = e.getValue().get();
			idx = index - start;
		}
		return page.get(idx);
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Override
	public void close() {
		if (_pages != null) {
			_pages.values().forEach((p) -> p._file.delete());
			_pages = null;
		}
	}

	public long size() {
		return _size;
	}

	public int pages() {
		if (_pages == null)
			throw new IllegalStateException("PagedIntBag is closed");
		return _pages.size();
	}

}
