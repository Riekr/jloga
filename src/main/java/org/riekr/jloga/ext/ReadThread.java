package org.riekr.jloga.ext;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

public class ReadThread extends Thread {

	private final    BufferedReader      _reader;
	private final    Consumer<String>    _consumer;
	private final    Consumer<Throwable> _errConsumer;
	private volatile boolean             _running = true;

	public ReadThread(@NotNull InputStream inputStream, @NotNull Consumer<String> consumer, @NotNull Consumer<Throwable> errConsumer, String tag) {
		super(tag + "-reader");
		setDaemon(true);
		_reader = new BufferedReader(new InputStreamReader(inputStream));
		_consumer = consumer;
		_errConsumer = errConsumer;
	}

	@Override
	public void run() {
		try {
			String line;
			while (_running && (line = _reader.readLine()) != null)
				_consumer.accept(line);
		} catch (Throwable e) {
			_errConsumer.accept(e);
		} finally {
			try {
				_reader.close();
			} catch (Throwable e) {
				e.printStackTrace(System.err);
			}
		}
	}

	@Override
	public void interrupt() {
		_running = false;
		super.interrupt();
	}

	public ReadThread startNow() {
		start();
		return this;
	}

	public void waitFor() {
		try {
			join();
		} catch (InterruptedException ignored) {}
	}
}
