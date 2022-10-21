package org.riekr.jloga.ui;

import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

public interface MenuSelectedListener extends MenuListener {
	@Override
	default void menuDeselected(MenuEvent e) {}

	@Override
	default void menuCanceled(MenuEvent e) {}
}
