package org.riekr.jloga.project;

import static java.lang.Boolean.FALSE;
import static org.riekr.jloga.utils.Utils.findKeyForValue;

import javax.swing.*;
import java.util.Map;

import org.riekr.jloga.search.SearchComponentWithExpandablePanel;

public class ProjectCheckboxField<T> extends ProjectField<T, JCheckBox> {

	private final Map<Boolean, T> _values;

	public ProjectCheckboxField(String key, String label, Map<Boolean, T> values) {
		super(key, label,
				(text, ui) -> values.get(Boolean.valueOf(text)),
				(value) -> findKeyForValue(values, value).orElse(FALSE).toString(),
				values.get(FALSE)
		);
		_values = values;
	}

	@Override
	protected JCheckBox newUI(SearchComponentWithExpandablePanel panel) {
		return panel.newCheckbox(key, label, this);
	}

	@Override
	public void set(T value) {
		super.set(value);
		_ui.setSelected(findKeyForValue(_values, _value).orElse(FALSE));
	}

	@Override
	public void accept(String s) {
		super.accept(s);
		_ui.setSelected(findKeyForValue(_values, _value).orElse(FALSE));
	}

}
