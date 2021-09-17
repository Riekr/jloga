package org.riekr.jloga.ui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.function.Supplier;

public class ContextMenu {

	private ContextMenu() {
	}

	public static <T extends JTextArea> T addActionCopy(T component) {
		return addActionCopy(component, () -> {
			String text = component.getSelectedText();
			if (text == null) {
				// try to fetch first highlight
				Highlighter highlighter = component.getHighlighter();
				if (highlighter != null) {
					Highlighter.Highlight[] highlights = highlighter.getHighlights();
					if (highlights != null && highlights.length > 0) {
						Highlighter.Highlight highlight = highlights[0];
						int start = highlight.getStartOffset();
						int end = highlight.getEndOffset();
						try {
							text = component.getText(start, end - start);
						} catch (BadLocationException e) {
							e.printStackTrace(System.err);
						}
					}
				}
			}
			return text;
		});
	}

	public static <T extends JLabel> T addActionCopy(T component) {
		return addActionCopy(component, component::getText);
	}

	public static <T extends JComponent> T addActionCopy(T component, Supplier<String> stringSupplier) {
		JPopupMenu popupMenu = ensurePopupMenu(component);
		popupMenu.add("Copy").addActionListener((a) -> {
			String text = stringSupplier.get();
			if (text == null)
				text = "";
			StringSelection stringSelection = new StringSelection(text);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);
		});
		return component;
	}

	public static JPopupMenu ensurePopupMenu(JComponent component) {
		JPopupMenu popupMenu = component.getComponentPopupMenu();
		if (popupMenu == null) {
			component.setInheritsPopupMenu(false);
			component.setComponentPopupMenu(popupMenu = new JPopupMenu());
		}
		return popupMenu;
	}

}
