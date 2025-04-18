package org.riekr.jloga.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.concurrent.atomic.AtomicReference;

public class JPopupMenuWithMouseLoc extends JPopupMenu {
	@Serial private static final long serialVersionUID = 4128574454805066731L;

	public final AtomicReference<Point> lastClick;

	public static JPopupMenuWithMouseLoc ensurePopupMenu(JComponent component) {
		JPopupMenuWithMouseLoc popupMenu = (JPopupMenuWithMouseLoc)component.getComponentPopupMenu();
		if (popupMenu == null) {
			AtomicReference<Point> lastClick = new AtomicReference<>();
			component.setInheritsPopupMenu(false);
			component.setComponentPopupMenu(popupMenu = new JPopupMenuWithMouseLoc(lastClick));
			component.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON3)
						lastClick.set(component.getMousePosition(false));
				}
			});
		}
		return popupMenu;
	}

	private JPopupMenuWithMouseLoc(AtomicReference<Point> lastClick) {
		this.lastClick = lastClick;
	}

	public Point getLastMouseClickPosition() {
		return lastClick.get();
	}

}
