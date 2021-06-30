package org.riekr.jloga.search;

import org.riekr.jloga.ui.MRUTextCombo;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegExComponent extends MRUTextCombo implements SearchComponent {

	private static class RegExSearch implements SearchPredicate {

		private final Pattern _pattern;
		private Matcher _matcher;

		public RegExSearch(Pattern pattern) {
			_pattern = pattern;
		}

		@Override
		public void start() {
			_matcher = _pattern.matcher("");
		}

		@Override
		public boolean accept(int line, String text) {
			_matcher.reset(text);
			return _matcher.find();
		}
	}

	public RegExComponent(int level) {
		super("regex." + level);
	}

	@Override
	public void onSearch(Consumer<SearchPredicate> consumer) {
		if (consumer == null)
			setListener(null);
		else {
			setListener((regex) -> {
				try {
					consumer.accept(new RegExSearch(Pattern.compile(regex)));
				} catch (PatternSyntaxException pse) {
					JOptionPane.showMessageDialog(this, pse.getLocalizedMessage(), "RegEx syntax error", JOptionPane.ERROR_MESSAGE);
				}
			});
		}
	}

	@Override
	public String getLabel() {
		return ".*";
//		return "\uD83C\uDD41";
	}
}
