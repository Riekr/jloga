package org.riekr.jloga.ui;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import org.riekr.jloga.prefs.Preferences;
import org.riekr.jloga.react.BoolConsumer;

public class HoverMonitor extends MouseAdapter implements PopupMenuListener, WindowFocusListener {

	private final BoolConsumer _consumer;
	private final Timer        _mouseEnter;
	private final Timer        _mouseExit;
	private       boolean      _mouseListenerEnabled = true;

	public HoverMonitor(BoolConsumer consumer) {
		this(consumer, 200, 300);
	}

	public HoverMonitor(BoolConsumer consumer, int enterDelay, int exitDelay) {
		_consumer = consumer;
		_mouseEnter = new Timer(enterDelay, e -> consumer.accept(true));
		_mouseEnter.setRepeats(false);
		_mouseExit = new Timer(exitDelay, e -> consumer.accept(false));
		_mouseExit.setRepeats(false);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		if (_mouseListenerEnabled && !Preferences.PRJCLICK.get()) {
			_mouseExit.stop();
			_mouseEnter.start();
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		if (_mouseListenerEnabled && !Preferences.PRJCLICK.get()) {
			_mouseEnter.stop();
			_mouseExit.start();
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (_mouseListenerEnabled && Preferences.PRJCLICK.get())
			_consumer.accept(true);
	}

	public boolean isEnabled() {
		return _mouseListenerEnabled;
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

	@Override
	public void windowGainedFocus(WindowEvent e) {}

	@Override
	public void windowLostFocus(WindowEvent e) {
		if (Preferences.PRJCLICK.get())
			_consumer.accept(false);
	}
}
