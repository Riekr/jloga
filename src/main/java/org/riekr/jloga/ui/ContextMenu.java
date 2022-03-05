package org.riekr.jloga.ui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ContextMenu {

	private ContextMenu() {
	}

	public static <T extends JTextArea> T addActionCopy(T component) {
		return addActionCopy(component, (p, a) -> {
			String text = component.getSelectedText();
			if (text != null && !text.isEmpty())
				return text;
			// try searching line number
			int viewToModel = component.viewToModel2D(p);
			if (viewToModel != -1) {
				try {
					int line = component.getLineOfOffset(viewToModel);
					int start = component.getLineStartOffset(line);
					int end = component.getLineEndOffset(line);
					return component.getText(start, end - start);
				} catch (BadLocationException e1) {
					e1.printStackTrace(System.err);
				}
			}
			// try to fetch first highlight (should not pass here)
			Highlighter highlighter = component.getHighlighter();
			if (highlighter != null) {
				Highlighter.Highlight[] highlights = highlighter.getHighlights();
				if (highlights != null && highlights.length > 0) {
					Highlighter.Highlight highlight = highlights[0];
					int start = highlight.getStartOffset();
					int end = highlight.getEndOffset();
					try {
						return component.getText(start, end - start);
					} catch (BadLocationException e) {
						e.printStackTrace(System.err);
					}
				}
			}
			return null;
		});
	}

	public static <T extends JLabel> T addActionCopy(T component) {
		return addActionCopy(component, (p, a) -> component.getText());
	}

	public static <T extends JComponent> T addActionCopy(T component, BiFunction<Point, ActionEvent, String> stringSupplier) {
		AtomicReference<Point> lastClick = new AtomicReference<>();
		JPopupMenu popupMenu = ensurePopupMenu(component, lastClick::set);
		popupMenu.add("Copy").addActionListener((a) -> {
			String text = stringSupplier.apply(lastClick.get(), a);
			if (text == null)
				text = "";
			StringSelection stringSelection = new StringSelection(text);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);
		});
		return component;
	}

	public static JPopupMenu ensurePopupMenu(JComponent component, Consumer<Point> lastClickConsumer) {
		JPopupMenu popupMenu = component.getComponentPopupMenu();
		if (popupMenu == null) {
			component.setInheritsPopupMenu(false);
			component.setComponentPopupMenu(popupMenu = new JPopupMenu());
			if (lastClickConsumer != null) {
				component.addMouseListener(new MouseAdapter() {
					@Override
					public void mousePressed(MouseEvent e) {
						if (e.getButton() == MouseEvent.BUTTON3)
							lastClickConsumer.accept(component.getMousePosition(false));
					}
				});
			}
		}
		return popupMenu;
	}

}
