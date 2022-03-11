package org.riekr.jloga.search;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.riekr.jloga.react.Unsubscribable;
import org.riekr.jloga.ui.MRUTextCombo;
import org.riekr.jloga.ui.utils.UIUtils;

public class UniqueSearchComponent extends Box implements SearchComponent {
	public static final String ID = "UniqueSearchComponent";

	private final MRUTextCombo<String> _combo;
	private       Unsubscribable       _comboListener;

	public UniqueSearchComponent(int level) {
		super(BoxLayout.LINE_AXIS);
		_combo = MRUTextCombo.newMRUTextCombo("unique." + level, "(\\w+(?:\\.\\w+)*\\.\\w*(?:Exception|Error))");
		add(_combo);
	}

	@Override
	public void onSearch(Consumer<SearchPredicate> consumer) {
		if (_comboListener != null) {
			_comboListener.unsubscribe();
			_comboListener = null;
		}
		if (consumer != null) {
			_comboListener = _combo.subject.subscribe((pattern) -> {
				Pattern pat = UIUtils.toPattern(this, pattern, 0, 0);
				if (pat != null)
					consumer.accept(new UniqueSearch(pat));
			});
		}
	}

	@Override
	public String getID() {
		return ID;
	}

	@Override
	public String getSearchIconLabel() {
		return "U";
	}
}
