package org.riekr.jloga.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.function.Supplier;

public class ContextMenu {

	private ContextMenu() {
	}

	public static <T extends JTextArea> T addActionCopy(T component) {
		return addActionCopy(component, component::getSelectedText);
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
