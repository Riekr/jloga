package org.riekr.jloga.utils;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntSupplier;

public class CaretLimiter implements CaretListener {

	public static void setup(JTextArea text, IntSupplier lastValidLine) {
		CaretLimiter limiter = new CaretLimiter(text, lastValidLine);
		text.addCaretListener(limiter);
		text.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {limiter.check();}
		});
	}

	private final JTextArea   _text;
	private final IntSupplier _lastValidLine;

	private boolean _updating;

	private CaretLimiter(JTextArea text, IntSupplier lastValidLine) {
		_text = text;
		_lastValidLine = lastValidLine;
	}

	@Override
	public void caretUpdate(CaretEvent e) {
		if (_updating)
			return;
		_updating = true;
		try {
			check();
		} finally {
			_updating = false;
		}
	}

	public void check() {
		try {
			int start = _text.getSelectionStart();
			int end = _text.getSelectionEnd();
			int lastValidLine = Integer.max(0, _lastValidLine.getAsInt());
			if (start != end) {
				int firstSelectedLine = _text.getLineOfOffset(start);
				int lastSelectedLine = _text.getLineOfOffset(end);
				// System.out.println("firstSelectedLine=" + firstSelectedLine + "\tlastSelectedLine=" + lastSelectedLine + "\tlastValidLine=" + lastValidLine);
				if (firstSelectedLine > lastValidLine)
					_text.getLineStartOffset(lastValidLine);
				if (lastSelectedLine > lastValidLine)
					_text.setSelectionEnd(_text.getLineEndOffset(lastValidLine));
			} else {
				int currentLine = _text.getLineOfOffset(start);
				// System.out.println("currentLine=" + currentLine + "\tlastValidLine=" + lastValidLine);
				if (currentLine > lastValidLine)
					_text.setCaretPosition(_text.getLineEndOffset(lastValidLine));
			}
		} catch (BadLocationException ex) {
			ex.printStackTrace(System.err);
		}
	}
}
