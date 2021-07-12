package org.riekr.jloga.search;

import org.riekr.jloga.misc.IntObjPredicate;
import org.riekr.jloga.ui.MRUTextCombo;
import org.riekr.jloga.ui.UIUtils;

import javax.swing.*;
import java.util.Locale;
import java.util.function.Consumer;

public class PlainTextComponent extends Box implements SearchComponent {

	private final MRUTextCombo _combo;
	private boolean _negate;
	private boolean _caseInsensitive;

	public PlainTextComponent(int level) {
		super(BoxLayout.LINE_AXIS);
		_combo = new MRUTextCombo("plainTextSearch." + level);
		add(_combo);
		add(UIUtils.newToggleButton("!", "Negate", false, (b) -> _negate = b));
		add(UIUtils.newToggleButton("\uD83D\uDDDA", "Case insensitive", false, (b) -> _caseInsensitive = b));
	}

	@Override
	public void onSearch(Consumer<SearchPredicate> consumer) {
		if (consumer == null)
			_combo.setListener(null);
		else {
			_combo.setListener((pattern) -> {
				if (pattern != null && !pattern.isEmpty()) {
					IntObjPredicate<String> predicate;
					if (_caseInsensitive) {
						String upperPattern = pattern.toUpperCase(Locale.ROOT);
						predicate = (line, text) -> text.toUpperCase(Locale.ROOT).contains(upperPattern);
					} else
						predicate = (line, text) -> text.contains(pattern);
					if (_negate)
						predicate = predicate.negate();
					consumer.accept(SearchPredicate.simple(predicate));
				}
			});
		}
	}

	@Override
	public String getLabel() {
		return "Tt";
	}
}
