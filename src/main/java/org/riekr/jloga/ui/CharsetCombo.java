package org.riekr.jloga.ui;

import static java.util.Objects.requireNonNull;

import javax.swing.*;
import java.io.Serial;
import java.nio.charset.Charset;

import org.riekr.jloga.prefs.Preferences;

public class CharsetCombo extends JComboBox<Charset> {
	@Serial private static final long serialVersionUID = -2526623806412379161L;

	public CharsetCombo() {
		super(Charset.availableCharsets().values().toArray(Charset[]::new));
		addItemListener(e -> Preferences.CHARSET.set((Charset)requireNonNull(getSelectedItem())));
	}

}
