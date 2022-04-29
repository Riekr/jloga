package org.riekr.jloga.project;

import static org.riekr.jloga.utils.Utils.findKeyForValue;

import java.util.Map;
import java.util.Set;

import org.riekr.jloga.ui.MRUComboWithLabels;

public class ProjectComboField<T> extends ProjectEditableField<T> {

	private final Map<String, T> _values;

	public ProjectComboField(String key, String label, Map<String, T> values) {
		super(key, label,
				(text, ui) -> values.get(text),
				(value) -> findKeyForValue(values, value).orElse(null),
				values.values().stream().findFirst().orElseThrow()
		);
		_values = values;
	}

	@Override
	protected MRUComboWithLabels<?> newUI(ProjectComponent panel) {
		MRUComboWithLabels<?> res = super.newUI(panel);
		res.combo.setEditable(false);
		res.combo.setSaveEnabled(false);
		try {
			Object sel = res.combo.getSelectedItem();
			res.combo.removeAllItems();
			Set<String> values = _values.keySet();
			values.forEach(res.combo::addItem);
			if (sel instanceof String && values.contains(sel))
				res.combo.setSelectedItem(sel);
		} finally {
			res.combo.setSaveEnabled(true);
		}
		res.combo.resend();
		return res;
	}

}
