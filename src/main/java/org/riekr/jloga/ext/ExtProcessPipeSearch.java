package org.riekr.jloga.ext;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;

import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TempTextSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.search.SearchException;
import org.riekr.jloga.search.SearchPredicate;

public class ExtProcessPipeSearch implements SearchPredicate {

	private final File     _workingDir;
	private final String[] _command;

	public ExtProcessPipeSearch(File workingDir, String... command) {
		if (command == null || command.length == 0)
			throw new IllegalArgumentException("No command specified");
		_workingDir = workingDir;
		_command = command;
	}

	private final String _LINE_SEP = System.lineSeparator();

	private volatile int            _line;
	private          TempTextSource _textSource;
	private          BufferedWriter _toProc;
	private          ReadThread     _stdOutReader;
	private          ReadThread     _stdErrReader;

	@Override
	public FilteredTextSource start(TextSource master) {
		if (_textSource != null || _toProc != null || _stdOutReader != null || _stdErrReader != null)
			throw new IllegalStateException("ExtProcessPipeSearch already started");
		Process process;
		try {
			System.out.println("RUNNING EXT: " + String.join(" ", _command));
			process = new ProcessBuilder(_command)
					.directory(_workingDir)
					.start();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		_toProc = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
		_textSource = new TempTextSource();
		_stdOutReader = new ReadThread(process.getInputStream(), this::onStdOut, "stdout").startNow();
		_stdErrReader = new ReadThread(process.getErrorStream(), this::onStdErr, "stderr").startNow();
		return _textSource;
	}

	// no need to increase visibility
	private void onStdOut(String line) {
		_textSource.addLine(_line, line);
	}

	protected void onStdErr(String line) {
		System.err.println(line);
	}

	@Override
	public void verify(int line, String text) {
		_line = line;
		try {
			_toProc.write(text);
			_toProc.write(_LINE_SEP);
			_toProc.flush();
		} catch (IOException e) {
			onStdErr("Child process exited (" + e.getLocalizedMessage() + ')');
			throw new SearchException();
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
		_stdOutReader.waitFor();
		_stdOutReader = null;
		_stdErrReader.waitFor();
		_stdErrReader = null;
		_textSource.complete();
		_textSource = null;
	}
}
