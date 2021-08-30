package org.riekr.jloga.misc;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class PagedIntBag implements Closeable {

	private static class Page {
		final File file;
		WeakReference<int[][]> data;

		Page(int[][] data) throws IOException {
			this.file = File.createTempFile("jloga", null);
			this.data = new WeakReference<>(data);
		}
	}

	private final int _depth;
	private int[][] _page;
	private final int _pageLen;

	private int _size = 0;
	private int _start = 0;
	private ArrayList<Page> _pageFiles = new ArrayList<>();

	public PagedIntBag(int depth) {
		if (depth <= 0)
			throw new IllegalArgumentException("Depth can't be <= 0");
		_depth = depth;
		// 2MB
		int pageSize = 1024 * 1024 * 2;
		_pageLen = (pageSize * depth) / Integer.BYTES;
		_page = new int[_pageLen][depth];
	}

	private void ensureOpen() {
		if (_pageFiles == null)
			throw new IllegalStateException("PagedIntBag is closed");
	}

	public void save() {
		ensureOpen();
		int pageId = _size / _pageLen;
		try {
			Page page;
			if (pageId == _pageFiles.size()) {
				page = new Page(_page);
				_pageFiles.add(page);
			} else {
				page = _pageFiles.get(pageId);
				page.data = new WeakReference<>(_page);
			}
			try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(page.file)))) {
				oos.writeObject(_page);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void load(int pageId) {
		ensureOpen();
		Page page = _pageFiles.get(pageId);
		if (page == null)
			throw new IllegalStateException("Page not created yet");
		if ((_page = page.data == null ? null : page.data.get()) != null) {
			System.out.println("Loading cached page " + pageId);
		} else {
			System.out.println("Loading page " + pageId);
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(page.file)))) {
				_page = (int[][]) ois.readObject();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		_start = _pageLen * pageId;
	}

	public void add(int... value) {
		if (value.length != _depth)
			throw new IllegalArgumentException("Invalid depth for value");
		int idx = _size - _start;
		if (idx == (_pageLen - 1)) {
			save();
			_start += _pageLen;
			idx = 0;
		}
		_page[idx] = value;
		_size++;
	}

	public int[] get(int index) {
		int idx = index - _start;
		if (idx < 0 || idx >= _page.length) {
			load(index / _pageLen);
			idx = index - _start;
		}
		return _page[idx];
	}

	@Override
	public void close() {
		if (_pageFiles == null)
			return;
		for (Page page : _pageFiles) {
			if (!page.file.delete())
				System.err.println("Unable to delete " + page.file);
			page.data = null;
		}
		_start = 0;
		_size = 0;
		_pageFiles = null;
	}

	public long size() {
		return _size;
	}

	public int pages() {
		return _pageFiles.size();
	}
}
