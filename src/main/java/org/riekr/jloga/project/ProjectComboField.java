package org.riekr.jloga.project;

import java.util.Map;

import org.riekr.jloga.search.SearchComponentWithExpandablePanel;
import org.riekr.jloga.ui.MRUComboWithLabels;

public class ProjectComboField<T> extends ProjectEditableField<T> {

	private final Map<String, T> _values;

	private static <T> String valueToKey(Map<String, T> values, T value) {
		return values.entrySet().stream().filter((e) -> e.getValue().equals(value)).findAny().map(Map.Entry::getKey).orElse(null);
	}

	public ProjectComboField(String key, String label, Map<String, T> values) {
		super(key, label,
				(text, ui) -> values.get(text),
				(value) -> valueToKey(values, value)
		);
		_values = values;
	}

	@Override
	protected MRUComboWithLabels<?> newUI(SearchComponentWithExpandablePanel panel) {
		MRUComboWithLabels<?> res = super.newUI(panel);
		res.combo.removeAllItems();
		_values.keySet().forEach(res.combo::addItem);
		res.combo.setEditable(false);
		res.combo.resend();
		return res;
	}

}
