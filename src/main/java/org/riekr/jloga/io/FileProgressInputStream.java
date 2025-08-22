package org.riekr.jloga.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import org.jetbrains.annotations.NotNull;

public class FileProgressInputStream extends FileInputStream {

	private final long             _size;
	private final ProgressListener _progressListener;
	private       long             _read;

	public FileProgressInputStream(@NotNull ProgressListener progressListener, @NotNull String name) throws FileNotFoundException {
		this(progressListener, new File(name));
	}

	public FileProgressInputStream(@NotNull ProgressListener progressListener, @NotNull File file) throws FileNotFoundException {
		super(file);
		_size = file.length();
		_progressListener = progressListener;
		progressListener.onProgressChanged(0, _size);
	}

	@Override
	public int read() throws IOException {
		final int res = super.read();
		if (res != -1) {
			_read++;
			_progressListener.onProgressChanged(_read, _size);
		}
		return res;
	}

	@Override
	public int read(byte @NotNull [] b) throws IOException {
		final int res = super.read(b);
		if (res != -1) {
			_read += res;
			_progressListener.onProgressChanged(_read, _size);
		}
		return res;
	}

	@Override
	public int read(byte @NotNull [] b, int off, int len) throws IOException {
		final int res = super.read(b, off, len);
		if (res != -1) {
			_read += res;
			_progressListener.onProgressChanged(_read, _size);
		}
		return res;
	}

	@Override
	public long transferTo(OutputStream out) throws IOException {
		final long read = _read;
		final long res = super.transferTo(out);
		if (res > 0) {
			_read = read + res;
			_progressListener.onProgressChanged(_read, _size);
		}
		return res;
	}

	@Override
	public long skip(long n) throws IOException {
		final long res = super.skip(n);
		if (res > 0) {
			_read += res;
			_progressListener.onProgressChanged(_read, _size);
		}
		return res;
	}

	@Override
	public void close() throws IOException {
		super.close();
		_progressListener.onProgressChanged(_size, _size);
	}
}
