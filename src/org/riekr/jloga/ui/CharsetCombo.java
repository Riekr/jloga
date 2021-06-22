package org.riekr.jloga.ui;

import org.riekr.jloga.io.Preferences;

import javax.swing.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;

public class CharsetCombo extends JComboBox<Charset> {

	public Charset charset;

	public CharsetCombo() {
		try {
			charset = Charset.forName(requireNonNull(Preferences.load(Preferences.CHARSET, StandardCharsets.UTF_8::name)));
		} catch (Throwable e) {
			charset = StandardCharsets.UTF_8;
		}
		for (Charset cs : Charset.availableCharsets().values())
			addItem(cs);
		setSelectedItem(charset);
		addItemListener(e -> {
			charset = (Charset) requireNonNull(getSelectedItem());
			Preferences.save(Preferences.CHARSET, charset.name());
		});
	}

}
