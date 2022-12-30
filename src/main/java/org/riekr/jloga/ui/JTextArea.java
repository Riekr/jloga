package org.riekr.jloga.ui;

import java.awt.*;

import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.utils.FontMetricsWrapper;

class JTextArea extends javax.swing.JTextArea {
	private static final long serialVersionUID = 5071286309976476695L;

	public JTextArea() {
		Preferences.LINEHEIGHT.subscribe(this, (val) -> this.repaint());
	}

	@Override
	public final FontMetrics getFontMetrics(Font font) {
		return new FontMetricsWrapper(super.getFontMetrics(font));
	}
}
