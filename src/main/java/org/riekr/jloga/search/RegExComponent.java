package org.riekr.jloga.search;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.riekr.jloga.misc.Predicates;
import org.riekr.jloga.misc.SearchComboEntry;
import org.riekr.jloga.react.Unsubscribable;
import org.riekr.jloga.search.simple.SimpleSearchPredicate;
import org.riekr.jloga.ui.MRUTextCombo;
import org.riekr.jloga.utils.UIUtils;

public class RegExComponent extends Box implements SearchComponent {
	private static final long serialVersionUID = -2681776106341733771L;

	public static final String ID = "RegExComponent";

	private final MRUTextCombo<SearchComboEntry> _combo;
	private       Unsubscribable                 _comboListener;

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
					if (pat.matcher("").groupCount() == 0)
						consumer.accept(SimpleSearchPredicate.FACTORY.from(Predicates.supplyFind(pat, value.negate)));
					else
						consumer.accept(new RegExSearch(pat));
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
		return ".*";
	}

	@Override
	public boolean requestFocusInWindow() {
		return _combo.requestFocusInWindow();
	}
}
