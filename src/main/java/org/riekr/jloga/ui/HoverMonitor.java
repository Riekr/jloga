package org.riekr.jloga.ui;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.riekr.jloga.react.BoolConsumer;

public class HoverMonitor extends MouseAdapter implements PopupMenuListener {

	private final Timer   _mouseEnter;
	private final Timer   _mouseExit;
	private       boolean _mouseListenerEnabled = true;

	public HoverMonitor(BoolConsumer consumer) {
		this(consumer, 200, 300);
	}

	public HoverMonitor(BoolConsumer consumer, int enterDelay, int exitDelay) {
		_mouseEnter = new Timer(enterDelay, e -> consumer.accept(true));
		_mouseEnter.setRepeats(false);
		_mouseExit = new Timer(exitDelay, e -> consumer.accept(false));
		_mouseExit.setRepeats(false);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		if (_mouseListenerEnabled) {
			_mouseExit.stop();
			_mouseEnter.start();
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		if (_mouseListenerEnabled) {
			_mouseEnter.stop();
			_mouseExit.start();
		}
	}

	public void setEnabled(boolean mouseListenerEnabled) {
		_mouseListenerEnabled = mouseListenerEnabled;
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		_mouseListenerEnabled = false;
	}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		_mouseListenerEnabled = true;
	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {
		_mouseListenerEnabled = true;
	}
}
