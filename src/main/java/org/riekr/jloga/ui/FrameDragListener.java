package org.riekr.jloga.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BooleanSupplier;

public class FrameDragListener extends MouseAdapter {

	public static void associate(Frame frame, Component component) {
		FrameDragListener listener = new FrameDragListener(frame, () -> (frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == 0);
		component.addMouseListener(listener);
		component.addMouseMotionListener(listener);
	}

	public static void associate(JDialog dialog, Component component) {
		FrameDragListener listener = new FrameDragListener(dialog, () -> true);
		component.addMouseListener(listener);
		component.addMouseMotionListener(listener);
	}

	private final Window          _window;
	private final BooleanSupplier _canStart;
	private       Point           _mouseDownCompCoords = null;

	private FrameDragListener(Window window, BooleanSupplier canStart) {
		_canStart = canStart;
		_window = window;
	}

	public void mouseReleased(MouseEvent e) {
		_mouseDownCompCoords = null;
	}

	public void mousePressed(MouseEvent e) {
		if (_canStart.getAsBoolean())
			_mouseDownCompCoords = _window.getMousePosition();
	}

	public void mouseDragged(MouseEvent e) {
		if (_mouseDownCompCoords != null) {
			Point currCoords = e.getLocationOnScreen();
			_window.setLocation(currCoords.x - _mouseDownCompCoords.x, currCoords.y - _mouseDownCompCoords.y);
		}
	}
}