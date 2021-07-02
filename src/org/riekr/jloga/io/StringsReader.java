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
		if (_start == -1) {
			_start = 0;
			cbuf[0] = '\n';
			return 1;
		}
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
	public void close() {
		_strings = null;
	}
}
