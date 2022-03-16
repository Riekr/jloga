package org.riekr.jloga.io;

import static java.util.Collections.singleton;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import org.jetbrains.annotations.NotNull;

public class StringsReader extends Reader {

	public static class ErrorReader extends StringsReader {
		public ErrorReader(@NotNull Throwable e) {
			super(singleton(e.getLocalizedMessage()).iterator());
		}
	}

	private Iterator<String> _strings;
	private int              _start = 0;

	public StringsReader(@NotNull Iterator<String> strings) {
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
		if (!_strings.hasNext())
			return -1;
		String string = _strings.next();
		if (string == null)
			return 0;
		int strLen = string.length();
		int strAvail = strLen - _start;
		if (strAvail < len)
			len = strAvail;
		string.getChars(_start, _start + len, cbuf, off);
		_start += len;
		if (_start == strLen)
			_start = -1;
		return len;
	}

	@Override
	public void close() {
		_strings = null;
	}
}
