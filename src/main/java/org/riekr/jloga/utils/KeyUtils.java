package org.riekr.jloga.utils;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class KeyUtils {

	public static <T extends Window & RootPaneContainer> void closeOnEscape(@NotNull T frame) {
		frame.getRootPane().registerKeyboardAction((e) -> frame.dispose(),
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	public static void addCtrlKeyAction(RootPaneContainer container, char key, Runnable action) {
		addCtrlKeyAction(container.getRootPane(), key, action);
	}

	public static void addCtrlKeyAction(JComponent component, char key, Runnable action) {
		component.registerKeyboardAction((e) -> action.run(),
				KeyStroke.getKeyStroke(key, InputEvent.CTRL_DOWN_MASK),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	private KeyUtils() {}
}
