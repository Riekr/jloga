package org.riekr.jloga.misc;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.util.ArrayList;

public class PagedIntBag implements Closeable {

	private final int _depth;

	private ArrayList<int[]> _data = new ArrayList<>();

	public PagedIntBag(int depth) {
		if (depth <= 0)
			throw new IllegalArgumentException("Depth can't be <= 0");
		_depth = depth;
	}

	private void ensureOpen() {
		if (_data == null)
			throw new IllegalStateException("PagedIntBag is closed");
	}

	public void save() {
		ensureOpen();
		// TODO:
	}

	public void add(int @NotNull ... value) {
		if (value.length != _depth)
			throw new IllegalArgumentException("Argument length must be " + _depth);
		ensureOpen();
		_data.add(value);
	}

	public int[] get(int index) {
		ensureOpen();
		return _data.get(index);
	}

	@Override
	public void close() {
		ensureOpen();
		_data = null;
		// TODO:
	}

	public long size() {
		return _data.size();
	}

	public int pages() {
		return 1;
	}
}
