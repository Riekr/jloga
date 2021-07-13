package org.riekr.jloga.misc;

import java.io.*;

public class PagedIntBag implements Closeable {

	private final int _depth;
	private final int[][] _page;
	private final int _pageSize = 1024 * 1024 * 2; // 2MB
	private final int _pageLen;

	private int _size = 0;
	private int _start = 0;
	private File _pageFile;

	public PagedIntBag(int depth) {
		if (depth <= 0)
			throw new IllegalArgumentException("Depth can't be <= 0");
		_depth = depth;
		_pageLen = (_pageSize * depth) / Integer.BYTES;
		_page = new int[_pageLen][depth];
	}

	private void save() {
		int page = _size / _pageLen;
		int pos = page * _pageSize;
		int len = _size - _start;
		try {
			if (_pageFile == null)
				_pageFile = File.createTempFile("jloga", null);
			try (RandomAccessFile raf = new RandomAccessFile(_pageFile, "rw")) {
				raf.seek(pos);
				for (int i = 0; i < len; i++) {
					for (int d = 0; d < _depth; d++)
						raf.writeInt(_page[i][d]);
				}
				for (int i = len; i < _pageLen; i++) {
					for (int d = 0; d < _depth; d++)
						raf.writeInt(0);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void load(int page) {
		if (_pageFile == null)
			throw new IllegalStateException("Page file not created yet");
		int pos = page * _pageSize;
		try (RandomAccessFile raf = new RandomAccessFile(_pageFile, "rw")) {
			raf.seek(pos);
			_start = _pageLen * (page + 1);
			for (int i = 0; i < _pageLen; i++) {
				for (int d = 0; d < _depth; d++)
					_page[i][d] = raf.readInt();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void add(int... value) {
		if (value.length != _depth)
			throw new IllegalArgumentException("Invalid depth for value");
		int idx = _size - _start;
		if (idx == _pageLen) {
			save();
			_start += _pageLen;
			idx = 0;
		}
		_page[idx] = value;
		_size++;
	}

	public int[] get(int index) {
		int idx = index - _start;
		if (idx < 0 || idx > _start + _pageLen) {
			load(_size / index);
			idx = index - _start;
		}
		return _page[idx];
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@Override
	public void close() {
		if (_pageFile != null) {
			_pageFile.delete();
			_pageFile = null;
		}
		_start = 0;
		_size = 0;
	}

	public int size() {
		return _size;
	}

	public int pages() {
		return (_size / _pageLen) + 1;
	}
}
