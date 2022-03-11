package org.riekr.jloga.search.custom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;

import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TempTextSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.search.SearchPredicate;

public class ExtProcessPipeSearch implements SearchPredicate {

	private final String[] _command;

	public ExtProcessPipeSearch(String... command) {
		if (command == null || command.length == 0)
			throw new IllegalArgumentException("No command specified");
		_command = command;
	}

	private final String _LINE_SEP = System.lineSeparator();

	private volatile int            _line;
	private          TempTextSource _textSource;
	private          BufferedWriter _toProc;
	private          Thread         _reader;

	@Override
	public FilteredTextSource start(TextSource master) {
		if (_textSource != null || _toProc != null || _reader != null)
			throw new IllegalStateException("ExtProcessPipeSearch already started");
		Process process;
		try {
			System.out.println("RUNNING EXT: " + String.join(" ", _command));
			process = new ProcessBuilder(_command).start();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		_toProc = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
		_reader = new Thread(() -> {
			try (BufferedReader fromProc = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = fromProc.readLine()) != null)
					_textSource.addLine(_line, line);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		_reader.setDaemon(true);
		_textSource = new TempTextSource();
		_reader.start(); // <- keep after assigning _textSource!
		return _textSource;
	}

	@Override
	public void verify(int line, String text) {
		_line = line;
		try {
			_toProc.write(text);
			_toProc.write(_LINE_SEP);
			_toProc.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void end() {
		if (_textSource == null || _toProc == null)
			throw new IllegalStateException("ExtProcessPipeSearch not started");
		try {
			_toProc.close();
		} catch (IOException ignored) {} finally {
			_toProc = null;
		}
		stopThread();
		_textSource.complete();
		_textSource = null;
	}

	@SuppressWarnings("deprecation")
	private void stopThread() {
		try {
			_reader.join();
		} catch (InterruptedException ignored) {
			_reader.stop();
		}
	}
}
