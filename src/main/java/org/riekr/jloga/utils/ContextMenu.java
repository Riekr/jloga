package org.riekr.jloga.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.ui.JPopupMenuWithMouseLoc;
import org.riekr.jloga.ui.VirtualTextArea;

public class ContextMenu {

	public static final String COPY   = "Copy";
	public static final String COPYLN = "Copy line number";

	private ContextMenu() {}

	public static void addActionCopy(@NotNull VirtualTextArea virtualTextArea, @NotNull JTextArea... components) {
		JTextArea component = components[0];
		for (JTextArea target : components) {
			addActionCopy(target, COPY, (p, a) -> {
				String text = component.getSelectedText();
				if (text != null && !text.isEmpty())
					return text;
				// try searching line number
				if (p != null) {
					text = TextAreaUtils.getTextAtMouseLocation(component, p);
					if (text != null && !text.isEmpty())
						return text;
				}
				// try to fetch first highlight (should not pass here)
				text = TextAreaUtils.getFirstHighlightedText(component);
				if (text != null && !text.isEmpty())
					return text;
				// nothing found
				return null;
			});
			addActionCopy(target, COPYLN, (p, a) -> {
				if (p != null) {
					int line = TextAreaUtils.getLineNumberAtMouseLocation(component, p);
					if (line != -1) {
						line += virtualTextArea.getFromLine();
						return Integer.toString(line);
					}
				}
				return null;
			});
		}
	}

	public static void addActionCopy(@NotNull JTextArea target) {
		addActionCopy(target, COPY, (p, a) -> {
			String text = target.getSelectedText();
			if (text != null && !text.isEmpty())
				return text;
			// try searching line number
			if (p != null) {
				text = TextAreaUtils.getTextAtMouseLocation(target, p);
				if (text != null && !text.isEmpty())
					return text;
			}
			// try to fetch first highlight (should not pass here)
			text = TextAreaUtils.getFirstHighlightedText(target);
			if (text != null && !text.isEmpty())
				return text;
			// nothing found
			return null;
		});
	}

	public static <T extends JLabel> T addActionCopy(T component) {
		return addActionCopy(component, COPY, (p, a) -> component.getText());
	}

	public static void addActionCopy(JComponent component, Object value) {
		if (value == null || (value instanceof CharSequence && ((CharSequence)value).length() == 0))
			return;
		if (value instanceof File) {
			addActionCopy(component, "Copy name", (p, a) -> ((File)value).getName());
			addActionCopy(component, "Copy absolute name", (p, a) -> ((File)value).getAbsolutePath());
			addActionCopy(component, "Copy parent dir", (p, a) -> ((File)value).getParentFile().getAbsolutePath());
		} else
			addActionCopy(component, COPY, (p, a) -> value.toString());
	}

	public static void addActionCopy(JComponent component, Supplier<? extends CharSequence> value) {
		addActionCopy(component, COPY, value);
	}

	public static void addActionCopy(JComponent component, String label, Supplier<? extends CharSequence> value) {
		addActionCopy(component, label, (p, a) -> {
			CharSequence val = value.get();
			return val == null || val.length() == 0 ? null : val.toString();
		});
	}

	public static <T extends JComponent> T addActionCopy(T component, String label, BiFunction<Point, ActionEvent, String> stringSupplier) {
		JPopupMenuWithMouseLoc popupMenu = JPopupMenuWithMouseLoc.ensurePopupMenu(component);
		popupMenu.add(label).addActionListener((a) -> {
			String text = stringSupplier.apply(popupMenu.getLastMouseClickPosition(), a);
			if (text != null) {
				StringSelection stringSelection = new StringSelection(text);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);
			}
		});
		return component;
	}

	public static <T extends JComponent> void addAction(T component, String label, Runnable action) {
		JPopupMenuWithMouseLoc popupMenu = JPopupMenuWithMouseLoc.ensurePopupMenu(component);
		popupMenu.add(label).addActionListener((a) -> action.run());
	}

}
