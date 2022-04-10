package org.riekr.jloga.ext;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TempTextSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.search.SearchException;
import org.riekr.jloga.search.SearchPredicate;

public class ExtProcessPipeSearch implements SearchPredicate {

	private final String _LINE_SEP = System.lineSeparator();

	private final File             _workingDir;
	private final List<String>     _command;
	private final Consumer<String> _onStdOut;

	private volatile int            _line;
	private          TempTextSource _textSource;
	private          BufferedWriter _toProc;
	private          ReadThread     _stdOutReader;
	private          ReadThread     _stdErrReader;
	private volatile Throwable      _err;
	private          Process        _process;

	public ExtProcessPipeSearch(@NotNull File workingDir, @NotNull List<String> command, @Nullable Pattern matchRegex) {
		if (command.isEmpty())
			throw new IllegalArgumentException("No command specified");
		_workingDir = workingDir;
		_command = command;
		if (matchRegex == null) {
			_onStdOut = (line) -> _textSource.addLine(_line, line);
		} else {
			Matcher matcher = matchRegex.matcher("");
			_onStdOut = (line) -> {
				try {
					matcher.reset(line);
					if (matcher.matches())
						_textSource.addLine(Integer.parseInt(matcher.group("line")) - 1, matcher.group("text"));
					else
						_textSource.addLine(_line, line);
				} catch (Throwable e) {
					e.printStackTrace(System.err);
				}
			};
		}
	}

	@Override
	public FilteredTextSource start(TextSource master) {
		if (_textSource != null || _toProc != null || _stdOutReader != null || _stdErrReader != null)
			throw new IllegalStateException("ExtProcessPipeSearch already started");
		try {
			System.out.println("RUNNING EXT: " + String.join(" ", _command));
			_process = new ProcessBuilder(_command)
					.directory(_workingDir)
					.start();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		_toProc = new BufferedWriter(new OutputStreamWriter(_process.getOutputStream()));
		_textSource = new TempTextSource();
		_stdOutReader = new ReadThread(_process.getInputStream(), _onStdOut, this::onError, "stdout").startNow();
		_stdErrReader = new ReadThread(_process.getErrorStream(), this::onStdErr, this::onError, "stderr").startNow();
		return _textSource;
	}

	private void onError(Throwable e) {
		_err = e;
	}

	// protected for catching lines in a dump window
	protected void onStdErr(String line) {
		System.err.println(line);
	}

	@Override
	public void verify(int line, String text) {
		if (_err == null) {
			_line = line;
			try {
				_toProc.write(text);
				_toProc.write(_LINE_SEP);
				_toProc.flush();
			} catch (IOException e) {
				_err = e;
			}
		}
		if (_err != null) {
			_stdOutReader.interrupt();
			_stdErrReader.interrupt();
			_process.destroy();
			_err.printStackTrace(System.err);
			onStdErr("Child process exited (" + _err.getLocalizedMessage() + ')');
			throw new SearchException("Input not fully processed", _err);
		}
	}

	@Override
	public void end(boolean interrupted) {
		if (interrupted)
			_process.destroy();
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
