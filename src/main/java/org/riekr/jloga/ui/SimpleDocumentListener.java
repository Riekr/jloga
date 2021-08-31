package org.riekr.jloga.ui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public interface SimpleDocumentListener extends DocumentListener {

	void onChange(DocumentEvent e);

	@Override
	default void insertUpdate(DocumentEvent e) {
		onChange(e);
	}

	@Override
	default void removeUpdate(DocumentEvent e) {
		onChange(e);
	}

	@Override
	default void changedUpdate(DocumentEvent e) {
		onChange(e);
	}
}
