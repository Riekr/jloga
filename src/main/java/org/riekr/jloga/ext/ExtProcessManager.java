package org.riekr.jloga.ext;

import static java.util.stream.Collectors.toList;
import static org.riekr.jloga.utils.KeyUtils.closeOnEscape;
import static org.riekr.jloga.utils.PopupUtils.popupError;
import static org.riekr.jloga.utils.TextUtils.replaceRegex;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.search.SearchPredicate;

public class ExtProcessManager {

	public static final Pattern VAR_PATTERN = Pattern.compile("%([\\w._]+)%");

	private final List<String> _command;
	private final File         _workingDirectory;
	private final Pattern      _matchRegex;
	private final Pattern      _sectionRegex;

	private Map<String, String> _searchVars;
	private Map<String, String> _allVars;

	private JTextArea     _stdErr;
	private List<String>  _lastCommand;
	private LocalDateTime _lastStart;

	public ExtProcessManager(@NotNull File workingDirectory, @NotNull List<String> command, @Nullable Pattern matchRegex, @Nullable Pattern sectionRegex) {
		_workingDirectory = workingDirectory;
		_command = command;
		_matchRegex = matchRegex;
		_sectionRegex = sectionRegex;
	}

	public SearchPredicate newSearchPredicate() {
		try {
			Map<String, String> env = getAllVars();
			_lastCommand = _command.stream()
					.map((param) -> replaceRegex(param, VAR_PATTERN, env))
					.filter((param) -> !param.isEmpty())
					.collect(toList());
			return new ExtProcessPipeSearch(_workingDirectory, _lastCommand, _matchRegex, _sectionRegex) {
				@Override
				public FilteredTextSource start(TextSource master) {
					_lastStart = LocalDateTime.now();
					return super.start(master);
				}

				@Override
				protected void onStdErr(String line) {
					super.onStdErr(line);
					EventQueue.invokeLater(() -> ExtProcessManager.this.onStdErr(line));
				}

				@Override
				public void end(boolean interrupted) {
					super.end(interrupted);
					EventQueue.invokeLater(() -> _stdErr = null);
				}
			};
		} catch (Exception ex) {
			popupError("Can't execute", ex);
			return null;
		}
	}

	private String toTime(LocalDateTime time) {
		if (time == null)
			return "";
		return "[" + time.format(DateTimeFormatter.ISO_LOCAL_TIME) + "] ";
	}

	private void onStdErr(String line) {
		if (_stdErr == null) {
			JFrame popup = new JFrame("Command error output:");
			_stdErr = new JTextArea();
			_stdErr.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
			_stdErr.setEditable(false);
			if (_lastCommand != null)
				_stdErr.setText(toTime(_lastStart) + "executed: " + String.join(" ", _lastCommand));
			popup.add(new JScrollPane(_stdErr));
			popup.setPreferredSize(new Dimension(640, 480));
			popup.pack();
			popup.setVisible(true);
			closeOnEscape(popup);
		}
		if (line.isEmpty())
			_stdErr.append("\n");
		else
			_stdErr.append('\n' + toTime(LocalDateTime.now()) + line);
	}

	public Map<String, String> getAllVars() {
		if (_allVars == null) {
			Map<String, String> res = new HashMap<>(System.getenv());
			if (_workingDirectory.isDirectory())
				ExtEnv.read(_workingDirectory, res);
			if (_searchVars != null)
				res.putAll(_searchVars);
			_allVars = res;
		}
		return _allVars;
	}

	public void setSearchVars(Map<String, String> searchVars) {
		_searchVars = searchVars;
		_allVars = null;
	}
}
