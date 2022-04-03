package org.riekr.jloga.utils;

import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.prefs.Preference;

public class KeyUtils {

	public static KeyStroke ESC = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

	public static KeyStroke CTRL_O     = KeyStroke.getKeyStroke('O', InputEvent.CTRL_DOWN_MASK);
	public static KeyStroke CTRL_COMMA = KeyStroke.getKeyStroke(',', InputEvent.CTRL_DOWN_MASK);
	public static KeyStroke CTRL_F     = KeyStroke.getKeyStroke('F', InputEvent.CTRL_DOWN_MASK);
	public static KeyStroke CTRL_R     = KeyStroke.getKeyStroke('R', InputEvent.CTRL_DOWN_MASK);
	public static KeyStroke CTRL_W     = KeyStroke.getKeyStroke('W', InputEvent.CTRL_DOWN_MASK);
	public static KeyStroke CTRL_DOT   = KeyStroke.getKeyStroke('.', InputEvent.CTRL_DOWN_MASK);

	public static void addKeyStrokeAction(RootPaneContainer container, KeyStroke key, Runnable action) {
		addKeyStrokeAction(container.getRootPane(), key, action);
	}

	public static void addKeyStrokeAction(RootPaneContainer container, Preference<KeyStroke> keyPref, Runnable action) {
		addKeyStrokeAction(container.getRootPane(), keyPref, action);
	}

	public static void addKeyStrokeAction(JComponent component, KeyStroke key, Runnable action) {
		addKeyStrokeAction(component, key, action, WHEN_IN_FOCUSED_WINDOW);
	}

	public static void addKeyStrokeAction(JComponent component, KeyStroke key, Runnable action, int when) {
		component.registerKeyboardAction((e) -> action.run(), key, when);
	}

	public static void addKeyStrokeAction(JComponent component, Preference<KeyStroke> keyPref, Runnable action) {
		addKeyStrokeAction(component, keyPref, action, WHEN_IN_FOCUSED_WINDOW);
	}

	public static void addKeyStrokeAction(JComponent component, Preference<KeyStroke> keyPref, Runnable action, int when) {
		keyPref.subscribe((key) -> {
			component.unregisterKeyboardAction(key);
			addKeyStrokeAction(component, key, action, when);
		});
	}

	public static <T extends Window & RootPaneContainer> void closeOnEscape(@NotNull T frame) {
		addKeyStrokeAction(frame, ESC, frame::dispose);
	}

	public static boolean isFunctionKey(int keyCode) {
		return Arrays.stream(KeyEvent.class.getFields())
				.filter((f) -> Modifier.isStatic(f.getModifiers()))
				.filter((f) -> f.getName().matches("VK_F\\d+"))
				.filter((f) -> f.getType().equals(Integer.TYPE))
				.map((f) -> {
					try {
						return (Integer)f.get(null);
					} catch (IllegalAccessException e) {
						return null;
					}
				})
				.filter(Objects::nonNull)
				.mapToInt(Integer::intValue)
				.anyMatch((iv) -> iv == keyCode);
	}

	public static boolean isSystemShortcut(int code, boolean ctrl, boolean alt, boolean shift) {
		// modifiers should be checked exclusively, they are esotic combos tough
		return /**/(ctrl && code == KeyEvent.VK_Q)
				||/* */(ctrl && code == KeyEvent.VK_C)
				||/* */(ctrl && code == KeyEvent.VK_V)
				||/* */(ctrl && code == KeyEvent.VK_X)
				||/* */(alt && code == KeyEvent.VK_F4)
				||/* */(alt && code == KeyEvent.VK_TAB)
				||/* */(shift && code == KeyEvent.VK_INSERT)
				||/* */(shift && code == KeyEvent.VK_CANCEL)
				||/* */(ctrl && code == KeyEvent.VK_INSERT);
	}

	private KeyUtils() {}
}
