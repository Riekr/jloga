package org.riekr.jloga.ui;

import static javax.swing.KeyStroke.getKeyStrokeForEvent;
import static org.riekr.jloga.prefs.KeyBindings.getGUIKeyBindings;
import static org.riekr.jloga.utils.TextUtils.describeKeyBinding;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.Objects;

import org.riekr.jloga.prefs.GUIPreference;
import org.riekr.jloga.utils.KeyUtils;

public class KeyStrokeToggleButton extends JToggleButton {
	private static final long serialVersionUID = 4621141411427607287L;

	private final GUIPreference<KeyStroke> _keyPref;
	private final List<AbstractButton>     _allButtons;
	private final JLabel                   _label = new JLabel();

	private final KeyListener _keyListener = new KeyAdapter() {

		private final Color _color = _label.getForeground();
		private KeyStroke _last;

		@Override
		public void keyPressed(KeyEvent e) {
			KeyStroke keyStroke = getKeyStrokeForEvent(e);
			if (!keyStroke.equals(_last)) {
				switch (e.getKeyCode()) {

					case KeyEvent.VK_SPACE:
						return;

					case KeyEvent.VK_ENTER:
						if (isValid(_last))
							_keyPref.set(_last);
						//noinspection fallthrough
					case KeyEvent.VK_ESCAPE:
						_label.setForeground(_color);
						setSelected(false);
						deselected();
						enableOthers();
						e.consume();
						break;

					default:
						printKeyStroke(keyStroke);
						if (isValid(keyStroke))
							_label.setForeground(_color);
						else
							_label.setForeground(Color.RED);
						break;
				}
			}
			_last = keyStroke;
		}
	};

	public KeyStrokeToggleButton(List<AbstractButton> allButtons, GUIPreference<KeyStroke> keyPref) {
		_allButtons = allButtons;
		_keyPref = keyPref;
		setFocusable(false);
		setLayout(new BorderLayout());
		add(new JLabel(keyPref.title()), BorderLayout.LINE_START);
		add(_label, BorderLayout.LINE_END);
		addActionListener(e -> {
			if (isSelected()) {
				selected();
				disableOthers();
			} else {
				deselected();
				enableOthers();
			}
		});
		printKeyStroke(_keyPref.get());
	}

	public void refresh() {
		printKeyStroke(_keyPref.get());
		if (isSelected()) {
			setSelected(false);
			enableOthers();
		}
	}

	private void printKeyStroke(KeyStroke keyStroke) {
		_label.setText("<html>" + describeKeyBinding(keyStroke, null) + "</html>");
	}

	protected boolean isValid(KeyStroke keyStroke) {
		int keyModifiers = keyStroke.getModifiers();
		boolean shift = (keyModifiers & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK;
		boolean ctrl = (keyModifiers & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;
		boolean alt = (keyModifiers & InputEvent.ALT_DOWN_MASK) == InputEvent.ALT_DOWN_MASK
				|| /*  */ (keyModifiers & InputEvent.ALT_GRAPH_DOWN_MASK) == InputEvent.ALT_GRAPH_DOWN_MASK;
		int keyCode = keyStroke.getKeyCode();
		return (ctrl || alt || KeyUtils.isFunctionKey(keyCode)) && !KeyUtils.isSystemShortcut(keyCode, ctrl, alt, shift) && !isInUse(keyStroke);
	}

	protected boolean isInUse(KeyStroke keyStroke) {
		return getGUIKeyBindings().stream().filter((pref) -> pref != _keyPref).anyMatch((pref) -> Objects.equals(pref.get(), keyStroke));
	}

	protected void selected() {
		addKeyListener(_keyListener);
		setFocusable(true);
		requestFocusInWindow();
	}

	protected void deselected() {
		printKeyStroke(_keyPref.get());
		removeKeyListener(_keyListener);
		setFocusable(false);
		getRootPane().requestFocus();
	}

	protected void disableOthers() {
		_allButtons.stream().filter((b) -> b != this).forEach((b) -> {
			b.setSelected(false);
			b.setEnabled(false);
		});
	}

	protected void enableOthers() {
		_allButtons.stream().filter((b) -> b != this).forEach((b) -> b.setEnabled(true));
	}
}
