package org.riekr.jloga.search;

import static org.riekr.jloga.react.Observer.uniq;
import static org.riekr.jloga.utils.UIUtils.newBorderlessButton;
import static org.riekr.jloga.utils.UIUtils.newToggleButton;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.misc.Predicates;
import org.riekr.jloga.misc.SearchComboEntry;
import org.riekr.jloga.react.Unsubscribable;
import org.riekr.jloga.search.simple.SimpleSearchPredicate;
import org.riekr.jloga.ui.MRUTextCombo;

public class PlainTextComponent extends Box implements SearchComponent {
	private static final long serialVersionUID = -2002183911884676582L;

	public static final String ID = "PlainTextComponent";

	private final MRUTextCombo<SearchComboEntry> _combo;
	private       Unsubscribable                 _comboListener;

	private final JToggleButton _negateBtn;
	private final JToggleButton _caseBtn;

	public PlainTextComponent(int level) {
		super(BoxLayout.LINE_AXIS);
		_negateBtn = newToggleButton("!", "Negate", false);
		_caseBtn = newToggleButton("\uD83D\uDDDA", "Case insensitive", false);
		_combo = new MRUTextCombo<>("plainTextSearch." + level, this::newEntry);
		_combo.selection.subscribe(uniq(this::updateButtons));
		updateButtons(_combo.getValue());
		add(_combo);
		add(newBorderlessButton("\uD83D\uDD0D", _combo::resend));
		add(_negateBtn);
		add(_caseBtn);
	}

	private void updateButtons(SearchComboEntry entry) {
		if (entry != null) {
			_negateBtn.setSelected(entry.negate);
			_caseBtn.setSelected(entry.caseInsensitive);
		}
	}

	private SearchComboEntry newEntry(String pattern, SearchComboEntry old) {
		SearchComboEntry res = new SearchComboEntry(pattern);
		if (old == null) {
			res.negate = _negateBtn.isSelected();
			res.caseInsensitive = _caseBtn.isSelected();
		} else {
			res.negate = old.negate;
			res.caseInsensitive = old.caseInsensitive;
		}
		return res;
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
					value.negate = _negateBtn.isSelected();
					value.caseInsensitive = _caseBtn.isSelected();
					consumer.accept(SimpleSearchPredicate.FACTORY.from(Predicates.supplyContains(
							value.pattern,
							value.caseInsensitive,
							value.negate
					)));
					_combo.save();
				}
			});
		}
	}

	@Override
	public String getID() {
		return ID;
	}

	@Override
	public String getSearchIconLabel() {
		return "Tt";
	}

	@Override
	public @NotNull JComponent getUIComponent() {
		return this;
	}

	@Override
	public boolean requestFocusInWindow() {
		return _combo.requestFocusInWindow();
	}
}
