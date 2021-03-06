package org.riekr.jloga.ui;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import org.riekr.jloga.prefs.KeyBindings;

public class ROKeyListener implements KeyListener {
	@Override
	public void keyTyped(KeyEvent e) {
		discard(e);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		discard(e);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		e.consume();
	}

	protected void discard(KeyEvent e) {
		//		System.out.println(e);
		switch (e.getKeyCode()) {
			case 27: // esc
				return;
			case 33: // pgup
				if (e.isControlDown())
					onPreviousTab();
				else
					onPageUp();
				break;
			case 34: // pgdown
				if (e.isControlDown())
					onNextTab();
				else
					onPageDn();
				break;
			case 35: // end
				if (e.isControlDown()) {
					onDocumentEnd();
					break;
				}
				//noinspection fallthrough
			case 36: // home
				if (e.isControlDown()) {
					onDocumentStart();
					break;
				}
				//noinspection fallthrough
			case 37:
			case 38: // su
			case 39:
			case 40: // giu
				// arrows
				return;
			case 44: // ,
			case 46: // .
			case 65: // A
			case 67: // C
			case 70: // F
			case 79: // O
			case 82: // R
			case 84: // T
			case 155: // ins
				if (e.isControlDown())
					return;
				break;
			case 115: // F4
			case 116: // F5
				return;
		}
		KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
		if (KeyBindings.getGUIKeyBindings().stream().noneMatch((pref) -> keyStroke.equals(pref.get())))
			e.consume();
	}

	protected void onDocumentStart() {}

	protected void onDocumentEnd() {}

	protected void onPageUp() {}

	protected void onPageDn() {}

	protected void onPreviousTab() {}

	protected void onNextTab() {}
}
