package org.riekr.jloga.transform;

import static org.riekr.jloga.utils.AsyncOperations.asyncTask;

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

	private Set<Integer>       _checkSet       = new HashSet<>();
	private FastSplitOperation _splitOperation = new FastSplitOperation();

	private int       _checkTarget = CHECK_LINES;
	private int       _colCount    = -1;
	private Boolean   _own;
	private Character _delim;

	private String  _header;
	private boolean _isAssured = true;

	public HeaderDetector(@Nullable HeaderDetector parent) {
		_parent = parent;
	}

	public void detect(@NotNull TextSource source, @Nullable Runnable onComplete) {
		// using "asyncIO" may lead to thread deadlock
		asyncTask(() -> {
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
			// I'm not waiting anymore, I can switch to IO thread
			source.defaultAsyncIO(() -> {
				try {
					for (int lineNumber = 0; lineNumber < _checkTarget && _checkSet != null; lineNumber++) {
						if (detect(lineNumber, source.getText(lineNumber)))
							break;
					}
				} catch (Throwable e) {
					e.printStackTrace(System.err);
				} finally {
					// finished the file
					complete();
					if (onComplete != null)
						EventQueue.invokeLater(onComplete);
				}
			});
		});
	}

	private synchronized void waitCompletion() {
		if (isPending()) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace(System.err);
			}
		}
	}

	private synchronized void complete() {
		try {
			_delim = _splitOperation.getDelim();
		} catch (IllegalStateException e) {
			_delim = null;
		}
		_splitOperation = null;
		_checkSet = null;
		if (_header == null || _colCount <= 1) {
			_header = "";
			if (_own == null)
				_own = false;
		} else if (_own == null)
			_own = true;
		notifyAll();
	}

	private boolean detect(int lineNumber, String line) {
		if (line == null || line.isEmpty() || _checkSet == null)
			return false;
		if (_checkSet.add(lineNumber)) {
			int count = _splitOperation.apply(line).length;
			if (_colCount == -1) {
				_colCount = count;
				if (_parent != null && _parent.requireComplete() && _parent._colCount == _colCount && _parent.isAssured()) {
					_header = _parent.getHeader();
					_own = false;
				} else {
					_own = true;
					_header = line;
				}
				return false;
			} else if (_colCount != count) {
				_isAssured = false;
				if (count > _colCount)
					_colCount = count;
				return false;
			}
			return _checkSet != null && _checkSet.size() >= _checkTarget;
		}
		return false;
	}

	private boolean requireComplete() {
		if (isPending())
			throw new IllegalStateException("Didn't finish checking");
		return true;
	}

	@NotNull
	public String getHeader() {
		requireComplete();
		return Objects.requireNonNull(_header);
	}

	public boolean isOwnHeader() {
		requireComplete();
		return Objects.requireNonNull(_own);
	}

	public boolean isPending() {
		return _checkSet != null || _own == null || _header == null;
	}

	public int getCheckTarget() {
		return _checkTarget;
	}

	public int getColCount() {
		requireComplete();
		return _colCount;
	}

	public void setAssured(boolean assured) {
		_isAssured = assured;
	}

	public boolean isAssured() {
		return _isAssured;
	}

	@Override public String toString() {
		return "HeaderDetector{" +
				"parent=" + _parent +
				", colCount=" + _colCount +
				", header='" + _header + '\'' +
				", own=" + _own +
				", delimiter='" + _delim + '\'' +
				", isAssured='" + _isAssured + '\'' +
				'}';
	}
}
