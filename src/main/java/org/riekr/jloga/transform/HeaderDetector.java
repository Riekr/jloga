package org.riekr.jloga.transform;

import java.awt.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

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
		source.defaultAsyncIO(() -> {
			int lineCount;
			try {
				lineCount = source.getLineCount();
			} catch (ExecutionException | InterruptedException e) {
				e.printStackTrace(System.err);
				return;
			}
			if (_parent != null)
				_parent.waitCompletion();
			_checkTarget = Math.min(CHECK_LINES, lineCount);
			for (int lineNumber = 0; lineNumber < _checkTarget && _checkSet != null; lineNumber++)
				detect(lineNumber, source.getText(lineNumber));
			// finished the file
			complete();
			if (onComplete != null)
				EventQueue.invokeLater(onComplete);
		});
	}

	private synchronized void waitCompletion() {
		if (!isComplete()) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace(System.err);
			}
		}
	}

	private synchronized void complete() {
		_checkSet = null;
		if (_header == null) {
			_header = "";
			if (_own == null)
				_own = false;
		} else if (_own == null)
			_own = true;
		notifyAll();
	}

	private void detect(int lineNumber, String line) {
		if (line == null || line.isEmpty() || _checkSet == null)
			return;
		if (_checkSet.add(lineNumber)) {
			int count = FastSplitOperation.count(line);
			if (_colCount == -1) {
				_colCount = count;
				if (_parent != null && _parent.requireComplete() && _parent._colCount == _colCount) {
					_header = _parent.getHeader();
					_own = false;
				} else {
					_own = true;
					_header = line;
				}
			} else if (_colCount != count) {
				_header = "";
				complete();
			}
			if (_checkSet != null && _checkSet.size() >= _checkTarget)
				complete();
		}
	}

	private boolean requireComplete() {
		if (!isComplete())
			throw new IllegalStateException("Didn't finish checking");
		return true;
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
