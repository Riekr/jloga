package org.riekr.jloga.ui;

import javax.swing.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CharsetCombo extends JComboBox<Charset> {

	public Charset charset = StandardCharsets.ISO_8859_1;

	public CharsetCombo() {
		for (Charset cs : Charset.availableCharsets().values())
			addItem(cs);
		setSelectedItem(charset);
	}

}
