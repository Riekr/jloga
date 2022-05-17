package org.riekr.jloga.utils;

import org.jetbrains.annotations.NotNull;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.Consumer;

public final class MouseListenerBuilder implements MouseListener {

	private static void nop(MouseEvent e) {}

	public static MouseListenerBuilder mouse() {
		return new MouseListenerBuilder();
	}

	private Consumer<MouseEvent> _mouseClicked  = MouseListenerBuilder::nop;
	private Consumer<MouseEvent> _mousePressed  = MouseListenerBuilder::nop;
	private Consumer<MouseEvent> _mouseReleased = MouseListenerBuilder::nop;
	private Consumer<MouseEvent> _mouseEntered  = MouseListenerBuilder::nop;
	private Consumer<MouseEvent> _mouseExited   = MouseListenerBuilder::nop;

	private MouseListenerBuilder() {}

	@Override public void mouseClicked(MouseEvent e) {_mouseClicked.accept(e);}

	@Override public void mousePressed(MouseEvent e) {_mousePressed.accept(e);}

	@Override public void mouseReleased(MouseEvent e) {_mouseReleased.accept(e);}

	@Override public void mouseEntered(MouseEvent e) {_mouseEntered.accept(e);}

	@Override public void mouseExited(MouseEvent e) {_mouseExited.accept(e);}

	public MouseListenerBuilder onClick(@NotNull Consumer<MouseEvent> consumer) {
		_mouseClicked = consumer;
		return this;
	}

	public MouseListenerBuilder onPress(@NotNull Consumer<MouseEvent> consumer) {
		_mousePressed = consumer;
		return this;
	}

	public MouseListenerBuilder onRelease(@NotNull Consumer<MouseEvent> consumer) {
		_mouseReleased = consumer;
		return this;
	}

	public MouseListenerBuilder onEnter(@NotNull Consumer<MouseEvent> consumer) {
		_mouseEntered = consumer;
		return this;
	}

	public MouseListenerBuilder onExit(@NotNull Consumer<MouseEvent> consumer) {
		_mouseExited = consumer;
		return this;
	}
}
