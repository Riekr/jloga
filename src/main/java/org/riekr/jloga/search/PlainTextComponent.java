package org.riekr.jloga.search;

import org.riekr.jloga.misc.IntObjPredicate;
import org.riekr.jloga.misc.SearchComboEntry;
import org.riekr.jloga.react.Unsubscribable;
import org.riekr.jloga.ui.MRUTextCombo;
import org.riekr.jloga.ui.UIUtils;

import javax.swing.*;
import java.util.Locale;
import java.util.function.Consumer;

public class PlainTextComponent extends Box implements SearchComponent {

	private final MRUTextCombo<SearchComboEntry> _combo;
	private Unsubscribable _comboListener;

	public PlainTextComponent(int level) {
		super(BoxLayout.LINE_AXIS);
		_combo = new MRUTextCombo<>("plainTextSearch." + level, SearchComboEntry::new);
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
				if (value != null && value.pattern != null && !value.pattern.isEmpty()) {
					final String pattern = value.pattern;
					IntObjPredicate<String> predicate;
					if (value.caseInsensitive) {
						String upperPattern = pattern.toUpperCase(Locale.ROOT);
						predicate = (line, text) -> text.toUpperCase(Locale.ROOT).contains(upperPattern);
					} else
						predicate = (line, text) -> text.contains(pattern);
					if (value.negate)
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
