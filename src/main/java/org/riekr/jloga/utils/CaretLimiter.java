package org.riekr.jloga.utils;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.util.function.IntSupplier;

public class CaretLimiter {

	public static void setup(JTextArea text, IntSupplier lastValidLineSupplier) {
		Runnable check = () -> {
			try {
				int start = text.getSelectionStart();
				int end = text.getSelectionEnd();
				int lastValidLine = Integer.max(0, lastValidLineSupplier.getAsInt());
				if (start != end) {
					int firstSelectedLine = text.getLineOfOffset(start);
					int lastSelectedLine = text.getLineOfOffset(end);
					// System.out.println("firstSelectedLine=" + firstSelectedLine + "\tlastSelectedLine=" + lastSelectedLine + "\tlastValidLine=" + lastValidLine);
					if (firstSelectedLine > lastValidLine)
						text.getLineStartOffset(lastValidLine);
					if (lastSelectedLine > lastValidLine)
						text.setSelectionEnd(text.getLineEndOffset(lastValidLine));
				} else {
					int currentLine = text.getLineOfOffset(start);
					// System.out.println("currentLine=" + currentLine + "\tlastValidLine=" + lastValidLine);
					if (currentLine > lastValidLine)
						text.setCaretPosition(text.getLineEndOffset(lastValidLine));
				}
			} catch (BadLocationException ex) {
				ex.printStackTrace(System.err);
			}
		};
		MouseMotionListener motionListener = new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				check.run();
			}
		};
		text.addCaretListener(new CaretListener() {
			private boolean _updating;

			@Override
			public void caretUpdate(CaretEvent e) {
				if (_updating)
					return;
				_updating = true;
				try {
					check.run();
				} finally {
					_updating = false;
				}
			}
		});
		text.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				check.run();
				text.addMouseMotionListener(motionListener);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				text.removeMouseMotionListener(motionListener);
			}
		});
	}

	private CaretLimiter() {}

}
