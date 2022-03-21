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
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.riekr.jloga.Main;
import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.search.SearchComponent;
import org.riekr.jloga.search.SearchPredicate;

public class ExtProcessComponent extends JPanel implements SearchComponent {
	private static final long serialVersionUID = 8599529986240844558L;

	private final String _id;
	private final String _icon;

	private Consumer<SearchPredicate> _searchPredicateConsumer;
	private Map<String, String>       _searchVars;
	private JTextArea                 _stdErr;
	private String[]                  _lastCommand;
	private LocalDateTime             _lastStart;

	public ExtProcessComponent(String id, String icon, String label, File workingDirectory, String[] command) {
		_id = id;
		_icon = icon;
		JButton launchButton = new JButton(label);
		add(launchButton);
		launchButton.addActionListener((e) -> {
			if (_searchPredicateConsumer != null) {
				try {
					_lastCommand = new String[command.length];
					Map<String, String> env = getAllVars(workingDirectory);
					Matcher mat = Pattern.compile("%([\\w_]+)%").matcher("");
					for (int i = 0; i < command.length; i++)
						_lastCommand[i] = replace(command[i], mat, env);
					_searchPredicateConsumer.accept(new ExtProcessPipeSearch(workingDirectory, _lastCommand) {
						@Override
						public FilteredTextSource start(TextSource master) {
							_lastStart = LocalDateTime.now();
							return super.start(master);
						}

						@Override
						protected void onStdErr(String line) {
							super.onStdErr(line);
							EventQueue.invokeLater(() -> ExtProcessComponent.this.onStdErr(line));
						}

						@Override
						public void end() {
							super.end();
							EventQueue.invokeLater(() -> _stdErr = null);
						}
					});
				} catch (Exception ex) {
					if (!(ex instanceof IllegalArgumentException))
						ex.printStackTrace(System.err);
					showMessageDialog(Main.getMain(), ex.getLocalizedMessage(), "Can't execute", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}

	private String replace(String orig, Matcher matcher, Map<String, String> env) {
		matcher.reset(orig);
		if (matcher.find()) {
			StringBuilder buf = new StringBuilder(orig.length());
			do {
				String key = matcher.group(1);
				String val = env.get(key);
				if (val == null)
					throw new IllegalArgumentException("Unbound variable name: " + key);
				matcher.appendReplacement(buf, val);
			} while (matcher.find());
			matcher.appendTail(buf);
			return buf.toString();
		}
		return orig;
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

	@Override
	public void onSearch(Consumer<SearchPredicate> consumer) {
		_searchPredicateConsumer = consumer;
	}

	@Override
	public String getID() {
		return _id;
	}

	@Override
	public String getSearchIconLabel() {
		return _icon;
	}

	@Override
	public void setVariables(Map<String, String> vars) {
		_searchVars = vars;
	}

	private Map<String, String> getAllVars(File workingDir) {
		Map<String, String> res = new HashMap<>(System.getenv());
		if (workingDir != null && workingDir.isDirectory())
			ExtEnv.read(workingDir, res);
		if (_searchVars != null)
			res.putAll(_searchVars);
		return res;
	}

}
