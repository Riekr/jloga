package org.riekr.jloga.io;

import org.riekr.jloga.react.IntBehaviourSubject;
import org.riekr.jloga.react.Unsubscribable;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.function.IntConsumer;

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

	private int _lineCount = 0;
	private final IntBehaviourSubject _lineCountSubject = new IntBehaviourSubject();

	private int _lastLine = -1;

	public FilteredTextSource(TextSource tie) {
		_tie = tie;
	}

	public void addLine(int line) {
		if (line <= _lastLine)
			throw new IllegalArgumentException("New line " + line + " must be <= of " + _lastLine);
		_lastLine = line;
		Map.Entry<Integer, Range> entry = _lines.floorEntry(line);
		if (entry == null) {
			_lines.put(_lineCount++, new Range(line));
			_lineCountSubject.next(_lineCount);
			return;
		}
		Range range = entry.getValue();
		Integer tieLine = range.getSrcLine(entry.getKey(), line - 1);
		if (tieLine == null) {
			_lines.put(_lineCount++, new Range(line));
			_lineCountSubject.next(_lineCount);
			return;
		}
		range.to++;
		_lineCount++;
		_lineCountSubject.next(_lineCount);
	}

	public void dispatchLineCount() {
		_lineCountSubject.next(_lineCount);
	}

	public void complete() {
		_lineCountSubject.last();
	}

	@Override
	public Unsubscribable requestLineCount(IntConsumer consumer) {
		return _lineCountSubject.subscribe(consumer);
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
		return _lineCount;
	}

	public Integer getSrcLine(int line) {
		Map.Entry<Integer, Range> entry = _lines.floorEntry(line);
		if (entry == null)
			return null;
		return entry.getValue().getSrcLine(entry.getKey(), line);
	}
}
