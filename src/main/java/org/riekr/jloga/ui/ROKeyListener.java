package org.riekr.jloga.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

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

	private void discard(KeyEvent e) {
//		System.out.println(e);
		switch (e.getKeyCode()) {
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
			case 36: // home
				if (e.isControlDown()) {
					onDocumentStart();
					break;
				}
			case 37:
			case 38: // su
			case 39:
			case 40: // giu
				// arrows
				return;
			case 65: // A
				if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) == KeyEvent.CTRL_DOWN_MASK)
					return;
			case 67: // C
			case 155: // ins
				if (e.isControlDown())
					return;
				break;
			case 115: // f4
				if (e.isAltDown() || e.isAltGraphDown())
					return;
				break;
		}
		e.consume();
	}

	protected void onDocumentStart() {
	}

	protected void onDocumentEnd() {
	}

	protected void onPageUp() {
	}

	protected void onPageDn() {
	}

	protected void onPreviousTab() {
	}

	protected void onNextTab() {
	}
}
