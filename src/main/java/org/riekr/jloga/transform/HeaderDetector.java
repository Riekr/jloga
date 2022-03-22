package org.riekr.jloga.transform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.TextSource;

public class HeaderDetector {

	public static final int CHECK_LINES = 25;

	private final @Nullable HeaderDetector _parent;

	private int          _checkTarget = CHECK_LINES;
	private Set<Integer> _checkSet    = new HashSet<>();
	private int          _colCount    = -1;
	private String       _header;
	private Boolean      _own;

	public HeaderDetector(@Nullable HeaderDetector parent) {
		_parent = parent;
	}

	public void detect(@NotNull TextSource source, @Nullable Runnable onComplete) {
		source.requestCompleteLineCount((lineCount) -> {
			_checkTarget = Math.min(CHECK_LINES, lineCount);
			// go async recursively to fullfill the number of lines to be checked
			requestDetect(source, 0, lineCount, onComplete);
		});
	}

	private void requestDetect(TextSource source, int from, int max, Runnable onComplete) {
		int count = _checkTarget - _checkSet.size();
		source.requestText(from, count, (reader) -> {
			detect(reader);
			if (!isComplete()) {
				int nextFrom = from + count;
				if (nextFrom < max) {
					requestDetect(source, nextFrom, max, onComplete);
					return;
				}
				// finished the file
				_checkSet = null;
			}
			if (onComplete != null)
				onComplete.run();
		});
	}

	public void detect(Reader reader) {
		try (BufferedReader br = reader instanceof BufferedReader ? (BufferedReader)reader : new BufferedReader(reader)) {
			String line;
			int lineNumber = 0;
			while ((line = br.readLine()) != null)
				detect(lineNumber++, line);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}

	public void detect(int lineNumber, String line) {
		if (line == null || line.isEmpty() || _checkSet == null)
			return;
		if (_checkSet.add(lineNumber)) {
			int count = FastSplitOperation.count(line);
			if (_colCount == -1) {
				_colCount = count;
				if (_parent != null && !_parent.isComplete())
					throw new IllegalStateException("Parent did not finish checking");
				if (_parent != null && _parent._colCount == _colCount) {
					_header = _parent.getHeader();
					_own = false;
				} else {
					_own = true;
					_header = line;
				}
			} else if (_colCount != count) {
				_checkSet = null;
				_header = "";
			}
			if (_checkSet != null && _checkSet.size() >= _checkTarget)
				_checkSet = null;
		}
	}

	private void requireComplete() {
		if (!isComplete())
			throw new IllegalStateException("Didn't finish checking");
	}

	@NotNull
	public String getHeader() {
		requireComplete();
		return Objects.requireNonNull(_header);
	}

	public boolean isOwnHeader() {
		if (!isComplete())
			throw new IllegalStateException("Didn't finish checking");
		return Objects.requireNonNull(_own);
	}

	public boolean isComplete() {
		return _checkSet == null && _own != null && _header != null;
	}

	public int getCheckTarget() {
		return _checkTarget;
	}
}
