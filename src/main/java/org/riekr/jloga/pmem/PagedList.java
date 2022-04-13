package org.riekr.jloga.pmem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PagedList<T> implements Closeable {

	class Page {
		private final     File                        _file;
		private @Nullable WeakReference<ArrayList<T>> _cache;
		final             int                         id;

		Page(File file, ArrayList<T> data, int id) {
			_file = file;
			this.id = id;
			// save
			try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(_file)))) {
				dos.writeInt(data.size());
				for (T obj : data)
					_encoder.accept(obj, dos);
				_cache = new WeakReference<>(data);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		public ArrayList<T> load() {
			ArrayList<T> res;
			String from = "memory";
			if (_cache == null || (res = _cache.get()) == null) {
				from = "disk";
				try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(_file)))) {
					int size = dis.readInt();
					res = new ArrayList<>(size);
					for (int i = 0; i < size; i++)
						res.add(_decoder.apply(dis));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				_cache = new WeakReference<>(res);
			}
			System.out.println("Loaded page " + id + " from " + from);
			return res;
		}
	}

	class Live {
		final ArrayList<T> data;
		final int          start;
		Page page;

		Live(ArrayList<T> data, int start) {
			this.data = data;
			this.start = start;
		}

		Live(@NotNull Page page, int start) {
			this.page = page;
			this.data = page.load();
			this.start = start;
		}
	}

	private final int _limit;

	private final @NotNull TreeMap<Integer, Page> _pages = new TreeMap<>();
	private final @NotNull DataEncoder<T>         _encoder;
	private final @NotNull DataDecoder<T>         _decoder;

	private volatile @Nullable Live _writing;
	private volatile @NotNull  Live _reading;
	private                    long _size = 0;

	public PagedList(int pageSizeLimit, @NotNull DataEncoder<T> encoder, @NotNull DataDecoder<T> decoder) {
		_encoder = encoder;
		_decoder = decoder;
		if (pageSizeLimit <= 0)
			throw new IllegalArgumentException("Invalid page limit size: " + pageSizeLimit);
		_limit = pageSizeLimit;
		Live initial = new Live(new ArrayList<>(_limit), 0);
		_writing = initial;
		_reading = initial;
	}

	private File createTempFile() {
		try {
			return File.createTempFile("jloga", Integer.toHexString(PagedList.this.hashCode()));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public synchronized void seal() {
		Live writing = _writing;
		if (writing != null) {
			_writing = null;
			if (!writing.data.isEmpty())
				save(writing);
		}
	}

	public boolean isSealed() {
		return _writing == null;
	}

	private void save(Live writing) {
		if (writing == null)
			throw new IllegalStateException("PagedList is sealed");
		writing.data.trimToSize();
		if (writing.page != null || _pages.put(writing.start, writing.page = new Page(createTempFile(), writing.data, _pages.size() + 1)) != null)
			throw new IllegalStateException("Page " + writing.start + " already saved");
	}

	public synchronized final void add(T value) {
		Live writing = _writing;
		if (writing == null)
			throw new IllegalStateException("PagedList is sealed");
		writing.data.add(value);
		_size++;
		if (writing.data.size() == _limit) {
			save(writing);
			_writing = new Live(new ArrayList<>(_limit), writing.start + writing.data.size());
		}
	}

	public synchronized T get(int index) {
		int idx = index - _reading.start;
		if (idx < 0 || idx >= _reading.data.size()) {
			Map.Entry<Integer, Page> e = _pages.floorEntry(index);
			if (e == null) {
				// no pages
				return null;
			}
			int newStart = e.getKey();
			idx = index - newStart;
			if (idx < _reading.data.size()) {
				_reading = new Live(e.getValue(), newStart);
			} else {
				Live writing = _writing;
				if (writing == null)
					throw new IndexOutOfBoundsException("Requested index " + idx + " while page size is " + _reading.data.size() + " (" + index + '/' + _size + ')');
				_reading = writing;
				idx = index - _reading.start;
			}
		}
		return _reading.data.get(idx);
	}

	@Override
	public synchronized void close() {
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
