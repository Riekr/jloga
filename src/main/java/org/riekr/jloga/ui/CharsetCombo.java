package org.riekr.jloga.ui;

import static java.util.Objects.requireNonNull;

import javax.swing.*;
import java.nio.charset.Charset;

import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.react.Unsubscribable;

public class CharsetCombo extends JComboBox<Charset> {
	private static final long serialVersionUID = -2526623806412379161L;

	private Unsubscribable _unsubscribable = Preferences.CHARSET.subscribe(this::setCharset);

	public Charset charset;

	public CharsetCombo() {
		charset = Preferences.CHARSET.get();
		for (Charset cs : Charset.availableCharsets().values())
			addItem(cs);
		setSelectedItem(charset);
		addItemListener(e -> {
			setCharset((Charset)requireNonNull(getSelectedItem()));
			if (_unsubscribable != null) {
				_unsubscribable.unsubscribe();
				_unsubscribable = null;
			}
		});
	}

	public void setCharset(Charset charset) {
		this.charset = charset;
	}

}
