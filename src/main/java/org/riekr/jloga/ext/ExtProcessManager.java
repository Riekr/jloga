package org.riekr.jloga.ext;

import static javax.swing.JOptionPane.showMessageDialog;
import static org.riekr.jloga.utils.KeyUtils.closeOnEscape;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.riekr.jloga.Main;
import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.search.SearchPredicate;
import org.riekr.jloga.utils.TextUtils;

public class ExtProcessManager {

	private final String[] _command;
	private final File     _workingDirectory;

	private Map<String, String> _searchVars;
	private JTextArea           _stdErr;
	private String[]            _lastCommand;
	private LocalDateTime       _lastStart;

	public ExtProcessManager(String[] command, File workingDirectory) {
		_command = command;
		_workingDirectory = workingDirectory;
	}

	public SearchPredicate newSearchPredicate() {
		try {
			_lastCommand = new String[_command.length];
			Map<String, String> env = getAllVars(_workingDirectory);
			Pattern pattern = Pattern.compile("%([\\w._]+)%");
			for (int i = 0; i < _command.length; i++)
				_lastCommand[i] = TextUtils.replaceRegex(_command[i], pattern, env);
			return new ExtProcessPipeSearch(_workingDirectory, _lastCommand) {
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
				public void end() {
					super.end();
					EventQueue.invokeLater(() -> _stdErr = null);
				}
			};
		} catch (Exception ex) {
			if (!(ex instanceof IllegalArgumentException))
				ex.printStackTrace(System.err);
			showMessageDialog(Main.getMain(), ex.getLocalizedMessage(), "Can't execute", JOptionPane.ERROR_MESSAGE);
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

	private Map<String, String> getAllVars(File workingDir) {
		Map<String, String> res = new HashMap<>(System.getenv());
		if (workingDir != null && workingDir.isDirectory())
			ExtEnv.read(workingDir, res);
		if (_searchVars != null)
			res.putAll(_searchVars);
		return res;
	}

	public void setSearchVars(Map<String, String> searchVars) {
		_searchVars = searchVars;
	}
}
