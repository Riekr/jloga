package org.riekr.jloga.ui;

import static java.util.Objects.requireNonNull;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.nio.charset.Charset;

import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.react.Unsubscribable;

public class CharsetCombo extends JComboBox<Charset> {
	private static final long serialVersionUID = -2526623806412379161L;

	private Unsubscribable _unsubscribable = Preferences.CHARSET.subscribe((charset) -> {
		setCharset(charset);
		setSelectedItem(charset);
	});

	public Charset charset;

	public CharsetCombo() {
		super(Charset.availableCharsets().values().toArray(Charset[]::new));
		addAncestorListener(new AncestorListener() {
			@Override
			public void ancestorAdded(AncestorEvent event) {}

			@Override
			public void ancestorRemoved(AncestorEvent event) {
				if (_unsubscribable != null) {
					_unsubscribable.unsubscribe();
					_unsubscribable = null;
				}
			}

			@Override
			public void ancestorMoved(AncestorEvent event) {}
		});
		addItemListener(e -> {
			setCharset((Charset)requireNonNull(getSelectedItem()));
			// TODO: for future implementation when this combo will depend on file specific charset
			// if (_unsubscribable != null) {
			// 	_unsubscribable.unsubscribe();
			// 	_unsubscribable = null;
			// }
		});
	}

	public void setCharset(Charset charset) {
		this.charset = charset;
	}

}
