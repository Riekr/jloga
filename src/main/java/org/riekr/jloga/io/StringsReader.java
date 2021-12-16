package org.riekr.jloga.io;

import java.io.IOException;
import java.io.Reader;

import org.jetbrains.annotations.NotNull;

public class StringsReader extends Reader {

	public static class ErrorReader extends StringsReader {
		public ErrorReader(@NotNull Throwable e) {
			super(new String[]{e.getLocalizedMessage()});
		}
	}

	private String[] _strings;
	private int      _i     = 0;
	private int      _start = 0;
	private int[]    _mark;

	public StringsReader(@NotNull String[] strings) {
		_strings = strings;
	}

	@Override
	public int read(char @NotNull [] cbuf, int off, int len) throws IOException {
		if (_start == -1) {
			_start = 0;
			cbuf[0] = '\n';
			return 1;
		}
		if (_strings == null)
			throw new IOException("Reader has been closed");
		if (_i == _strings.length)
			return -1;
		String string = _strings[_i];
		int strLen = string.length();
		int strAvail = strLen - _start;
		if (strAvail < len)
			len = strAvail;
		string.getChars(_start, _start + len, cbuf, off);
		_start += len;
		if (_start == strLen) {
			_i++;
			_start = -1;
		}
		return len;
	}

	@Override
	public void mark(int readAheadLimit) {
		_mark = new int[]{_i, _start};
	}

	@Override
	public void reset() {
		if (_mark == null) {
			_i = 0;
			_start = 0;
		} else {
			_i = _mark[0];
			_start = _mark[1];
		}
	}

	@Override
	public void close() {
		_strings = null;
	}
}
