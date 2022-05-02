package org.riekr.jloga.search;

import static org.riekr.jloga.react.Observer.uniq;
import static org.riekr.jloga.utils.UIUtils.horizontalBox;
import static org.riekr.jloga.utils.UIUtils.newBorderlessButton;
import static org.riekr.jloga.utils.UIUtils.newToggleButton;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.misc.Predicates;
import org.riekr.jloga.misc.SearchComboEntry;
import org.riekr.jloga.react.Unsubscribable;
import org.riekr.jloga.search.simple.SimpleSearchPredicate;
import org.riekr.jloga.ui.MRUTextCombo;
import org.riekr.jloga.utils.UIUtils;

public class RegExComponent extends JComponent implements SearchComponent {
	private static final long serialVersionUID = -2681776106341733771L;

	public static final String ID = "RegExComponent";

	private final MRUTextCombo<SearchComboEntry> _combo;
	private       Unsubscribable                 _comboListener;

	private final JToggleButton _negateBtn;
	private final JToggleButton _caseBtn;

	public RegExComponent(int level) {
		setLayout(new BorderLayout());
		_negateBtn = newToggleButton("!", "Negate", false);
		_caseBtn = newToggleButton("\uD83D\uDDDA", "Case insensitive", false);
		_combo = new MRUTextCombo<>("regex." + level, this::newEntry);
		_combo.selection.subscribe(uniq(this::updateButtons));
		updateButtons(_combo.getValue());
		add(_combo, BorderLayout.CENTER);
		add(horizontalBox(
				newBorderlessButton("\uD83D\uDD0D", _combo::resend),
				_negateBtn,
				_caseBtn
		), BorderLayout.LINE_END);
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
				Pattern pat = UIUtils.toPattern(this, value.pattern, 0, value.caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
				if (pat != null) {
					value.negate = _negateBtn.isSelected();
					value.caseInsensitive = _caseBtn.isSelected();
					if (pat.matcher("").groupCount() == 0)
						consumer.accept(SimpleSearchPredicate.FACTORY.from(Predicates.supplyFind(pat, value.negate)));
					else
						consumer.accept(new RegExSearch(pat));
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
		return ".*";
	}

	@Override
	public @NotNull JComponent getUIComponent() {
		return this;
	}

	@Override
	public boolean requestFocusInWindow() {
		return _combo.requestFocusInWindow();
	}

	@Override
	public void prefill(String text) {
		if (text != null) {
			text = "\\Q" + text + "\\E";
			for (int i = 0, len = _combo.getItemCount(); i < len; i++) {
				SearchComboEntry entry = _combo.getItemAt(i);
				if (entry != null && text.equals(entry.pattern)) {
					_combo.setSelectedIndex(i);
					return;
				}
			}
			_combo.setSelectedItem(new SearchComboEntry(text));
		}
	}
}
