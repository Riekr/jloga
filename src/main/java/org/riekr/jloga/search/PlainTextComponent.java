package org.riekr.jloga.search;

import javax.swing.*;
import java.util.function.Consumer;

import org.riekr.jloga.misc.Predicates;
import org.riekr.jloga.misc.SearchComboEntry;
import org.riekr.jloga.react.Unsubscribable;
import org.riekr.jloga.search.simple.SimpleSearchPredicate;
import org.riekr.jloga.ui.MRUTextCombo;
import org.riekr.jloga.ui.utils.UIUtils;

public class PlainTextComponent extends Box implements SearchComponent {

	private final MRUTextCombo<SearchComboEntry> _combo;
	private       Unsubscribable                 _comboListener;

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
					consumer.accept(SimpleSearchPredicate.FACTORY.from(Predicates.supplyContains(
							value.pattern,
							value.caseInsensitive,
							value.negate
					)));
				}
			});
		}
	}

	@Override
	public String getLabel() {
		return "Tt";
	}
}
