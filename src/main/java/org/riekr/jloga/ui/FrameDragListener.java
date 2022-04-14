package org.riekr.jloga.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FrameDragListener extends MouseAdapter {

	public static void associate(JFrame frame, Component component) {
		FrameDragListener listener = new FrameDragListener(frame);
		component.addMouseListener(listener);
		component.addMouseMotionListener(listener);
	}

	private final JFrame frame;
	private       Point  mouseDownCompCoords = null;

	private FrameDragListener(JFrame frame) {
		this.frame = frame;
	}

	public void mouseReleased(MouseEvent e) {
		mouseDownCompCoords = null;
	}

	public void mousePressed(MouseEvent e) {
		if ((frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == 0) {
			mouseDownCompCoords = frame.getMousePosition();
		}
	}

	public void mouseDragged(MouseEvent e) {
		if (mouseDownCompCoords != null) {
			Point currCoords = e.getLocationOnScreen();
			frame.setLocation(currCoords.x - mouseDownCompCoords.x, currCoords.y - mouseDownCompCoords.y);
		}
	}
}