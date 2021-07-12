package org.riekr.jloga.search;

import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.ui.MRUTextCombo;
import org.riekr.jloga.ui.UIUtils;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExComponent extends Box implements SearchComponent {

	private static class RegExSearch extends SearchPredicate.Simple {

		private final Pattern _pattern;
		private Matcher _matcher;

		public RegExSearch(Pattern pattern) {
			_pattern = pattern;
		}

		@Override
		public FilteredTextSource start(TextSource master) {
			_matcher = _pattern.matcher("");
			return super.start(master);
		}

		@Override
		public boolean accept(int line, String text) {
			_matcher.reset(text);
			return _matcher.find();
		}
	}

	private final MRUTextCombo _combo;
	private boolean _negate;
	private boolean _caseInsensitive;

	public RegExComponent(int level) {
		super(BoxLayout.LINE_AXIS);
		_combo = new MRUTextCombo("regex." + level);
		add(_combo);
		add(UIUtils.newToggleButton("!", "Negate", false, (b) -> _negate = b));
		add(UIUtils.newToggleButton("\uD83D\uDDDA", "Case insensitive", false, (b) -> _caseInsensitive = b));
	}

	@Override
	public void onSearch(Consumer<SearchPredicate> consumer) {
		if (consumer == null)
			_combo.setListener(null);
		else {
			_combo.setListener((regex) -> {
				Pattern pat = UIUtils.toPattern(this, regex, 0, _caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
				if (pat != null) {
					if (_negate)
						consumer.accept(new RegExSearch(pat) {
							@Override
							public boolean accept(int line, String text) {
								return !super.accept(line, text);
							}
						});
					else
						consumer.accept(new RegExSearch(pat));
				}
			});
		}
	}

	@Override
	public String getLabel() {
		return ".*";
	}
}
