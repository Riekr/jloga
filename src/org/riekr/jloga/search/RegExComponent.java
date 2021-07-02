package org.riekr.jloga.search;

import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.ui.MRUTextCombo;
import org.riekr.jloga.ui.UIUtils;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExComponent extends MRUTextCombo implements SearchComponent {

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

	public RegExComponent(int level) {
		super("regex." + level);
	}

	@Override
	public void onSearch(Consumer<SearchPredicate> consumer) {
		if (consumer == null)
			setListener(null);
		else {
			setListener((regex) -> {
				Pattern pat = UIUtils.toPattern(this, regex, 0);
				if (pat != null)
					consumer.accept(new RegExSearch(pat));
			});
		}
	}

	@Override
	public String getLabel() {
		return ".*";
//		return "\uD83C\uDD41";
	}
}
