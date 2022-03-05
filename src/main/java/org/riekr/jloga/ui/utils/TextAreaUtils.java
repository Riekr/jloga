package org.riekr.jloga.ui.utils;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import java.awt.*;

import org.jetbrains.annotations.NotNull;

public class TextAreaUtils {

	public static int getLineNumberAtMouseLocation(@NotNull JTextArea component, @NotNull Point p) {
		int viewToModel = component.viewToModel2D(p);
		if (viewToModel != -1) {
			try {
				return component.getLineOfOffset(viewToModel);
			} catch (BadLocationException e) {
				e.printStackTrace(System.err);
			}
		}
		return -1;
	}

	public static String getTextAtMouseLocation(@NotNull JTextArea component, @NotNull Point p) {
		int line = getLineNumberAtMouseLocation(component, p);
		if (line != -1)
			return getTextAtLine(component, line);
		return null;
	}

	public static String getTextAtLine(@NotNull JTextArea component, int line) {
		try {
			int start = component.getLineStartOffset(line);
			int end = component.getLineEndOffset(line);
			return component.getText(start, end - start);
		} catch (BadLocationException e) {
			e.printStackTrace(System.err);
		}
		return null;
	}

	public static String getFirstHighlightedText(@NotNull JTextArea component) {
		Highlighter highlighter = component.getHighlighter();
		if (highlighter != null) {
			Highlighter.Highlight[] highlights = highlighter.getHighlights();
			if (highlights != null && highlights.length > 0) {
				Highlighter.Highlight highlight = highlights[0];
				try {
					int start = highlight.getStartOffset();
					int end = highlight.getEndOffset();
					return component.getText(start, end - start);
				} catch (BadLocationException e) {
					e.printStackTrace(System.err);
				}
			}
		}
		return null;

	}

	private TextAreaUtils() {}

}
