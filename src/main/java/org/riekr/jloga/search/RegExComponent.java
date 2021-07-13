package org.riekr.jloga.search;

import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.misc.SearchComboEntry;
import org.riekr.jloga.react.Unsubscribable;
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

	private final MRUTextCombo<SearchComboEntry> _combo;
	private Unsubscribable _comboListener;

	public RegExComponent(int level) {
		super(BoxLayout.LINE_AXIS);
		_combo = new MRUTextCombo<>("regex." + level, SearchComboEntry::new);
		add(_combo);
		SearchComboEntry initialValue = _combo.getValue();
		JToggleButton negateBtn = UIUtils.newToggleButton("!", "Negate", initialValue.negate, (b) -> {
			_combo.getValue().negate = b;
			_combo.save();
		});
		JToggleButton caseBtn = UIUtils.newToggleButton("\uD83D\uDDDA", "Case insensitive", initialValue.caseInsensitive, (b) -> {
			_combo.getValue().caseInsensitive = b;
			_combo.save();
		});
		_combo.subject.subscribe((value) -> {
			negateBtn.setSelected(value.negate);
			caseBtn.setSelected(value.caseInsensitive);
		});
		add(negateBtn);
		add(caseBtn);
	}

	@Override
	public void onSearch(Consumer<SearchPredicate> consumer) {
		if (_comboListener != null) {
			_comboListener.unsubscribe();
			_comboListener = null;
		}
		if (consumer != null) {
			_comboListener = _combo.subject.subscribe((value) -> {
				Pattern pat = UIUtils.toPattern(this, value.pattern, 0, value.caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
				if (pat != null) {
					if (value.negate)
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
