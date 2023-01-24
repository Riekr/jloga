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
import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.utils.TempFiles;

public class PagedList<T> implements Closeable {

	class Page {
		private final     File                        _file;
		private @Nullable WeakReference<ArrayList<T>> _cache;
		// private final     int                         _id = _pages.size() + 1;

		Page(File file, byte[] buf, ArrayList<T> data) throws IOException {
			_file = file;
			// save
			try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(_file)))) {
				dos.writeInt(data.size());
				dos.write(buf);
			}
			_cache = new WeakReference<>(data);
		}

		public ArrayList<T> load() {
			ArrayList<T> res;
			// String from = "memory";
			if (_cache == null || (res = _cache.get()) == null) {
				// from = "disk";
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
			// System.out.println("Loaded page " + _id + " from " + from);
			return res;
		}
	}

	class Live {
		private final ArrayList<T> _data;

		final long start;

		private Page                      _page;
		private ByteArrayDataOutputStream _buf;

		Live(long start) {
			_data = new ArrayList<>(1000);
			this.start = start;
			_buf = new ByteArrayDataOutputStream(_pageSize);
		}

		Live(@NotNull Page page, long start) {
			_data = page.load();
			_page = page;
			this.start = start;
			_buf = null;
		}

		private Page page() {
			if (_page != null)
				throw new IllegalStateException("Already paged");
			try {
				_page = new Page(createTempFile(), _buf.toByteArray(), _data);
				_buf.close();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			_buf = null;
			return _page;
		}

		public boolean isEmpty() {
			return _data.isEmpty();
		}

		public boolean add(T value) {
			if (_buf == null)
				throw new IllegalStateException("Already paged");
			_data.add(value);
			try {
				_encoder.accept(_buf, value);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			return _buf.size() >= _pageSize;
		}

		public int size() {
			return _data.size();
		}

		public T get(int idx) {
			return _data.get(idx);
		}
	}

	private final          int                 _pageSize = Preferences.PAGE_SIZE.get();
	private final @NotNull TreeMap<Long, Page> _pages    = new TreeMap<>();
	private final @NotNull DataEncoder<T>      _encoder;
	private final @NotNull DataDecoder<T>      _decoder;

	private volatile @Nullable Live _writing;
	private volatile @NotNull  Live _reading;
	private                    long _size = 0;
	private                    File _tempDir;

	public PagedList(@NotNull DataEncoder<T> encoder, @NotNull DataDecoder<T> decoder) {
		_encoder = encoder;
		_decoder = decoder;
		Live initial = new Live(0);
		_writing = initial;
		_reading = initial;
	}

	private File createTempFile() {
		if (_tempDir == null)
			_tempDir = TempFiles.createTempDirectory("PagedList");
		return TempFiles.createTempFile("Page", _tempDir);
	}

	public synchronized void seal() {
		Live writing = _writing;
		if (writing != null) {
			_writing = null;
			if (!writing.isEmpty())
				save(writing);
		}
	}

	public boolean isSealed() {
		return _writing == null;
	}

	private void save(Live writing) {
		if (writing == null)
			throw new IllegalStateException("PagedList is sealed");
		if (_pages.put(writing.start, writing.page()) != null)
			throw new IllegalStateException("Duplicate writing start " + writing.start);
	}

	public synchronized final void add(T value) {
		Live writing = _writing;
		if (writing == null)
			throw new IllegalStateException("PagedList is sealed");
		if (writing.add(value)) {
			save(writing);
			_writing = new Live(writing.start + writing.size());
		}
		_size++;
	}

	public synchronized T get(long index) {
		int idx = Math.toIntExact(index - _reading.start);
		if (idx < 0 || idx >= _reading.size()) {
			Map.Entry<Long, Page> e = _pages.floorEntry(index);
			if (e == null) {
				// no pages
				return null;
			}
			long newStart = e.getKey();
			idx = Math.toIntExact(index - newStart);
			_reading = new Live(e.getValue(), newStart);
			if (idx >= _reading.size()) {
				Live writing = _writing;
				if (writing == null)
					throw new IndexOutOfBoundsException("Requested index " + idx + " while page size is " + _reading.size() + " (" + index + '/' + _size + ')');
				_reading = writing;
				idx = Math.toIntExact(index - writing.start);
			}
		}
		return _reading.get(idx);
	}

	@Override
	public synchronized void close() {
		if (_tempDir != null)
			TempFiles.deleteTemp(_tempDir);
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
