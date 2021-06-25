package org.riekr.jloga.io;

import org.jetbrains.annotations.NotNull;

import java.io.Reader;

public class StringsReader extends Reader {

	private String[] _strings;
	private int _i = 0;
	private int _start = 0;

	public StringsReader(String[] strings) {
		_strings = strings;
	}

	@Override
	public int read(char @NotNull [] cbuf, int off, int len) {
		if (_i == _strings.length)
			return -1;
		String string = _strings[_i];
		int strLen = string.length();
		if (_start == strLen) {
			_i++;
			if (_i == _strings.length)
				return -1;
			_start = 0;
			cbuf[off] = '\n';
			return 1;
		}
		final int readLen = strLen - _start;
		final int end = _start + Math.min(len, readLen);
		string.getChars(_start, end, cbuf, off);
		_start = end;
		return len;
	}

	@Override
	public void close() {
		_strings = null;
	}
}
