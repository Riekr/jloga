package org.riekr.jloga.search.custom;

import javax.swing.*;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.riekr.jloga.search.SearchComponent;
import org.riekr.jloga.search.SearchPredicate;

public class ExtProcessComponent extends JPanel implements SearchComponent {

	private final String                    _id;
	private final String                    _icon;
	private       Consumer<SearchPredicate> _searchPredicateConsumer;
	private       Map<String, String>       _vars;

	public ExtProcessComponent(String id, String icon, String label, String[] command) {
		_id = id;
		_icon = icon;
		JButton launchButton = new JButton(label);
		add(launchButton);
		launchButton.addActionListener((e) -> {
			if (_searchPredicateConsumer != null) {
				String[] escapedCommand = new String[command.length];
				Map<String, String> env = System.getenv();
				Matcher mat = Pattern.compile("%([\\w_]+)%").matcher("");
				for (int i = 0; i < command.length; i++)
					escapedCommand[i] = replace(command[i], mat, env);
				_searchPredicateConsumer.accept(new ExtProcessPipeSearch(escapedCommand));
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