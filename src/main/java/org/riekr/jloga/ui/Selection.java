package org.riekr.jloga.ui;

import static java.lang.Math.max;
import static java.lang.Math.min;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.util.function.IntSupplier;

public class Selection implements CaretListener {

	public int     length;
	public Integer startLine, startColumn;
	public Integer endLine, endColumn;
	public boolean enabled = true;

	private final IntSupplier offsetSupplier;
	private       boolean     _skipNext;

	public Selection(IntSupplier offsetSupplier) {
		this.offsetSupplier = offsetSupplier;
	}

	@Override
	public void caretUpdate(CaretEvent event) {
		if (_skipNext) {
			_skipNext = false;
			return;
		}
		if (enabled) {
			// System.out.println(event);
			Object source = event.getSource();
			if (source instanceof JTextArea)
				updateFrom((JTextArea)source);
			else
				System.err.println("Invalid caret event on " + source);
		}
	}

	private void updateFrom(JTextArea textArea) {
		try {
			int start = textArea.getSelectionStart();
			int end = textArea.getSelectionEnd();
			this.length = end - start;
			if (this.length == 0) {
				this.startLine = this.startColumn = null;
				this.endLine = this.endColumn = null;
			} else {
				int startLine = textArea.getLineOfOffset(start);
				int startColumn = start - textArea.getLineStartOffset(startLine);
				int endLine = textArea.getLineOfOffset(end);
				int endColumn = end - textArea.getLineStartOffset(endLine);
				int offset = offsetSupplier.getAsInt();
				// commit
				this.startLine = offset + startLine;
				this.startColumn = startColumn;
				this.endLine = offset + endLine;
				this.endColumn = endColumn;
			}
		} catch (Throwable e) {
			e.printStackTrace(System.err);
		}
		// System.out.println(this);
	}

	public void restore(int minVisibleLine, int visibleLines, JTextArea textArea) {
		if (length == 0)
			return;
		try {
			int startLine = this.startLine - minVisibleLine;
			int endLine = this.endLine - minVisibleLine;
			if (startLine < 0 && endLine < 0)
				return;
			int maxLine = minVisibleLine + visibleLines;
			if (startLine > maxLine && endLine > maxLine)
				return;
			int start = startLine < 0 ? 0 : textArea.getLineStartOffset(startLine) + startColumn;
			int end = textArea.getLineStartOffset(endLine) + endColumn;
			int max = textArea.getText().length();
			// System.out.println(start + " - " + end + " - " + max);
			if (start < 0 && end < 0)
				return;
			if (start > max && end > max)
				return;
			_skipNext = true;
			textArea.setSelectionStart(max(start, 0));
			_skipNext = true;
			textArea.setSelectionEnd(min(end, max));
		} catch (Throwable e) {
			e.printStackTrace(System.err);
		}
	}

	@Override public String toString() {
		return "Selection{" +
				"length=" + length +
				", startLine=" + startLine +
				", startColumn=" + startColumn +
				", endLine=" + endLine +
				", endColumn=" + endColumn +
				'}';
	}

}
