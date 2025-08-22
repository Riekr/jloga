package org.riekr.jloga.ui;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Vector;

import org.jetbrains.annotations.NotNull;

public class ComboBoxWithLabelAndTooltips<T> extends Box {

	private record Entry(
			Object value,
			String name,
			String tooltip
	) {
		@Override
		public @NotNull String toString() {return name;}
	}

	private final Vector<Entry>    _data     = new Vector<>();
	private final JLabel           _label    = new JLabel();
	private final JComboBox<Entry> _comboBox = new JComboBox<>(_data);

	public ComboBoxWithLabelAndTooltips() {
		super(BoxLayout.X_AXIS);
		add(_label);
		_comboBox.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED)
				_comboBox.setToolTipText(((Entry)e.getItem()).tooltip);
		});
		add(_comboBox);
	}

	public ComboBoxWithLabelAndTooltips(String labelText, String tooltip) {
		this();
		_label.setText(labelText);
		setToolTipText(tooltip);
	}

	public ComboBoxWithLabelAndTooltips<T> setLabel(String labelText) {
		_label.setText(labelText);
		return this;
	}

	public ComboBoxWithLabelAndTooltips<T> setGlobalTooltip(String tooltip) {
		setToolTipText(tooltip);
		return this;
	}

	public ComboBoxWithLabelAndTooltips<T> addValue(T value, String name) {
		return addValue(value, name, "");
	}

	public ComboBoxWithLabelAndTooltips<T> addValue(T value, String name, String tooltip) {
		_data.add(new Entry(value, name, tooltip));
		return this;
	}

	public ComboBoxWithLabelAndTooltips<T> addDefaultValue(T value, String name) {
		return addDefaultValue(value, name, "");
	}

	public ComboBoxWithLabelAndTooltips<T> addDefaultValue(T value, String name, String tooltip) {
		addValue(value, name, tooltip);
		_comboBox.setSelectedIndex(_data.size() - 1);
		_comboBox.setToolTipText(tooltip);
		return this;
	}

	public void setValue(T value) {
		_comboBox.setSelectedItem(value);
	}

	@SuppressWarnings("unchecked") public T getValue() {
		final Entry selectedItem = (Entry)_comboBox.getSelectedItem();
		return selectedItem == null ? null : (T)selectedItem.value;
	}

}
