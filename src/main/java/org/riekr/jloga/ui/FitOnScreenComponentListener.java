package org.riekr.jloga.ui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class FitOnScreenComponentListener extends ComponentAdapter {

	public static final FitOnScreenComponentListener INSTANCE = new FitOnScreenComponentListener();

	private FitOnScreenComponentListener() {}

	@Override
	public void componentShown(ComponentEvent e) {
		Component comp = e.getComponent();
		Point origin = comp.getLocationOnScreen();
		int dx = 0, dy = 0;
		if (origin.x < 0)
			dx -= origin.x;
		if (origin.y < 0)
			dy -= origin.y;
		Dimension size = comp.getSize();
		Dimension screenSize = comp.getGraphicsConfiguration().getBounds().getSize();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(comp.getGraphicsConfiguration());
		screenSize.width -= insets.left + insets.right;
		screenSize.height -= insets.top + insets.bottom;
		// working if it fits only!
		dx -= Math.max(0, origin.x + dx + size.width - screenSize.width);
		dy -= Math.max(0, origin.y + dy + size.height - screenSize.height);
		if (dx != 0 || dy != 0) {
			Point loc = comp.getLocationOnScreen();
			loc.x += dx;
			loc.y += dy;
			comp.setLocation(loc);
		}
	}

}
