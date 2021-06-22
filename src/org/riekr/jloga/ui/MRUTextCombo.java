package org.riekr.jloga.ui;

import org.riekr.jloga.io.Preferences;

import javax.swing.*;
import java.util.function.Consumer;

public class MRUTextCombo extends JComboBox<String> {

	private final DefaultComboBoxModel<String> _model;

	private Consumer<String> _listener;

	public MRUTextCombo(String key) {
		super();
		_model = Preferences.loadDefaultComboBoxModel(key);
		setModel(_model);
		setEditable(true);
		addActionListener(e -> {
			String elem = (String) getSelectedItem();
			switch (e.getActionCommand()) {
				case "comboBoxEdited":
					_model.removeElement(elem);
					_model.insertElementAt(elem, 0);
					Preferences.save(key, _model);
					break;
				case "comboBoxChanged":
					if (_listener != null)
						_listener.accept((String) getSelectedItem());
					break;
				default:
					System.err.println(e);
			}
		});
	}

	public void setListener(Consumer<String> listener) {
		_listener = listener;
	}
}
