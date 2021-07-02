package org.riekr.jloga.ui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class FitOnScreenComponentListener extends ComponentAdapter {

	public static final FitOnScreenComponentListener INSTANCE = new FitOnScreenComponentListener();

	private FitOnScreenComponentListener() {
	}

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
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenSize = toolkit.getScreenSize();
		Insets insets = toolkit.getScreenInsets(comp.getGraphicsConfiguration());
		// try to fix hidpi monitor
		float k = toolkit.getScreenResolution() / 96.0f;
		screenSize.width -= (insets.left + insets.right) / k;
		screenSize.height -= (insets.top + insets.bottom) / k;
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
