package org.riekr.jloga.ui.utils;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class KeyUtils {

	public static void closeOnEscape(JDialog dialog) {
		dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	private KeyUtils() {}
}
