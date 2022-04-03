package org.riekr.jloga.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.riekr.jloga.io.TextSource;
import org.riekr.jloga.react.BoolConsumer;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class HoverMonitor extends MouseAdapter implements PopupMenuListener {

	private final BoolConsumer _consumer;
	private final long         _enterDelay, _exitDelay;

	private Future<?> _mouseEnterdFuture, _mouseExitedFuture;
	private boolean _mouseListenerEnabled = true;

	public HoverMonitor(BoolConsumer consumer) {
		this(consumer, 200, 300);
	}

	public HoverMonitor(BoolConsumer consumer, long enterDelay, long exitDelay) {
		_consumer = consumer;
		_enterDelay = enterDelay;
		_exitDelay = exitDelay;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		if (_mouseListenerEnabled) {
			if (_mouseExitedFuture == null || !_mouseExitedFuture.cancel(false)) {
				if (_mouseEnterdFuture == null || _mouseEnterdFuture.isDone()) {
					_mouseEnterdFuture = TextSource.MONITOR_EXECUTOR.schedule(() -> {
						_mouseEnterdFuture = null;
						_consumer.accept(true);
					}, _enterDelay, TimeUnit.MILLISECONDS);
				}
			}
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		if (_mouseListenerEnabled) {
			if (_mouseEnterdFuture == null || !_mouseEnterdFuture.cancel(false)) {
				if (_mouseExitedFuture == null || _mouseExitedFuture.isDone()) {
					_mouseExitedFuture = TextSource.MONITOR_EXECUTOR.schedule(() -> {
						_mouseExitedFuture = null;
						_consumer.accept(false);
					}, _exitDelay, TimeUnit.MILLISECONDS);
				}
			}
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
