package org.riekr.jloga.utils;

import org.drjekyll.fontchooser.FontDialog;

import javax.swing.*;
import java.awt.*;

public class FontUtils {

	public static String describeFont(Font font) {
		if (font == null)
			return null;
		String family = font.getFamily();
		String name = font.getName().replace('.', ' ');
		if (name.toUpperCase().startsWith(family.toUpperCase()))
			name = name.substring(family.length()).trim();
		return family + ' ' + name + ' ' + font.getSize();
	}

	public static Font selectFontDialog(Dialog parent, Font initialFont) {
		FontDialog dialog = new FontDialog(parent, "Select Font", true);
		dialog.setSelectedFont(initialFont);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setVisible(true);
		if (!dialog.isCancelSelected())
			return dialog.getSelectedFont();
		return null;
	}

	private FontUtils() {}
}
