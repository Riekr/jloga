package org.riekr.jloga.io;

import org.jetbrains.annotations.NotNull;

import java.io.Reader;

public class CharSequenceReader extends Reader {

	private final CharSequence _buf;
	private int _pos;

	public CharSequenceReader(CharSequence buf) {
		_buf = buf;
	}

	@Override
	public int read(char @NotNull [] cbuf, int off, int len) {
		int avail = _buf.length() - 1 - _pos;
		if (avail == 0)
			return -1;
		int readLen = Math.min(avail, len);
		for (int i = 0; i < readLen; i++)
			cbuf[off + i] = _buf.charAt(_pos++);
		return readLen;
	}

	@Override
	public void close() {
	}
}
