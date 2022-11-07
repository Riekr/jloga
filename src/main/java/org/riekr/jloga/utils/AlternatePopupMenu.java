package org.riekr.jloga.utils;

import static java.awt.EventQueue.invokeLater;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

import org.riekr.jloga.ui.TextIcon;

public class AlternatePopupMenu extends MouseAdapter {

	@SafeVarargs
	public static void setup(JComponent component, Map<String, Runnable>... actionMaps) {
		JPopupMenu menu = new JPopupMenu();
		for (Map<String, Runnable> actions : actionMaps) {
			actions.forEach((label, action) -> {
				JMenuItem menuItem = new JMenuItem(label);
				if (label.equalsIgnoreCase("edit"))
					menuItem.setIcon(new TextIcon(menuItem, "\uD83D\uDD89"));
				menuItem.addActionListener((evt) -> invokeLater(action));
				menu.add(menuItem);
			});
		}
		setup(component, menu);
	}

	public static void setup(JComponent component, JPopupMenu alternatePopupMenu) {
		component.addMouseListener(new AlternatePopupMenu(component, alternatePopupMenu));
	}

	private AlternatePopupMenu(JComponent component, JPopupMenu alternatePopupMenu) {
		_component = component;
		_alternatePopupMenu = alternatePopupMenu;
	}

	private final JComponent _component;
	private final JPopupMenu _alternatePopupMenu;
	private       JPopupMenu _mainPopupMenu;

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON3) {
			JPopupMenu popupMenu = _component.getComponentPopupMenu();
			if (popupMenu != null && popupMenu != _alternatePopupMenu) {
				_mainPopupMenu = popupMenu;
				_component.setComponentPopupMenu(_alternatePopupMenu);
			}
		} else if (_mainPopupMenu != null)
			_component.setComponentPopupMenu(_mainPopupMenu);
	}

}
