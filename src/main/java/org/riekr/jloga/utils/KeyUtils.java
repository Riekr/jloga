package org.riekr.jloga.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class KeyUtils {

	public static <T extends Window & RootPaneContainer> void closeOnEscape(T frame) {
		frame.getRootPane().registerKeyboardAction(e -> frame.dispose(),
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	private KeyUtils() {}
}
