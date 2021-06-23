package org.riekr.jloga.io;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

public class FilteredTextSource implements TextSource {

	private static class Range {
		final int from;
		int to;

		private Range(int from) {
			this.from = from;
			this.to = from;
		}

		public Integer getSrcLine(int base, int line) {
			int offset = line - base;
			int res = from + offset;
			return res > to ? null : res;
		}
	}

	private final TextSource _tie;
	private final TreeMap<Integer, Range> _lines = new TreeMap<>();
	private int _size = 0;

	public FilteredTextSource(TextSource tie) {
		_tie = tie;
	}

	public void addLine(int line) {
		Map.Entry<Integer, Range> entry = _lines.floorEntry(line);
		if (entry == null) {
			_lines.put(_size++, new Range(line));
			return;
		}
		Range range = entry.getValue();
		Integer tieLine = range.getSrcLine(entry.getKey(), line - 1);
		if (tieLine == null) {
			_lines.put(_size++, new Range(line));
			return;
		}
		range.to++;
		_size++;
	}

	@Override
	public String getText(int line) throws ExecutionException, InterruptedException {
		Integer tieLine = getSrcLine(line);
		if (tieLine != null)
			return _tie.getText(tieLine);
		return "";
	}

	@Override
	public int getLineCount() {
		return _size;
	}

	public Integer getSrcLine(int line) {
		Map.Entry<Integer, Range> entry = _lines.floorEntry(line);
		if (entry == null)
			return null;
		return entry.getValue().getSrcLine(entry.getKey(), line);
	}
}
