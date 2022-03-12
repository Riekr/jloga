package org.riekr.jloga.ext;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.search.SearchComponent;
import org.riekr.jloga.search.SearchPredicate;

public class ExtProcessComponent extends JPanel implements SearchComponent {

	private final String _id;
	private final String _icon;

	private Consumer<SearchPredicate> _searchPredicateConsumer;
	private Map<String, String>       _vars;
	private JTextArea                 _stdErr;
	private String[]                  _lastCommand;
	private LocalDateTime             _lastStart;

	public ExtProcessComponent(String id, String icon, String label, String[] command) {
		_id = id;
		_icon = icon;
		JButton launchButton = new JButton(label);
		add(launchButton);
		launchButton.addActionListener((e) -> {
			if (_searchPredicateConsumer != null) {
				_lastCommand = new String[command.length];
				Map<String, String> env = System.getenv();
				Matcher mat = Pattern.compile("%([\\w_]+)%").matcher("");
				for (int i = 0; i < command.length; i++)
					_lastCommand[i] = replace(command[i], mat, env);
				_searchPredicateConsumer.accept(new ExtProcessPipeSearch(_lastCommand) {
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
			}
		});
	}

	private String replace(String orig, Matcher matcher, Map<String, String> env) {
		matcher.reset(orig);
		if (matcher.find()) {
			StringBuilder buf = new StringBuilder(orig.length());
			do {
				String key = matcher.group(1);
				String val;
				if (_vars == null)
					val = env.get(key);
				else {
					val = _vars.get(key);
					if (val == null)
						val = env.get(key);
				}
				if (val == null)
					throw new IllegalArgumentException("Invalid env name: " + key);
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
		_vars = vars;
	}
}
