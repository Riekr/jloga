package org.riekr.jloga.theme;

import javax.swing.*;
import java.awt.*;

import org.riekr.jloga.io.VolatileTextSource;
import org.riekr.jloga.ui.VirtualTextArea;

public class ThemePreview extends JPanel {
	private static final long serialVersionUID = -7755998013962966312L;

	public ThemePreview() {
		super(new BorderLayout());
		VirtualTextArea textArea = new VirtualTextArea.Dummy("Lorem ipsum");
		add(textArea, BorderLayout.CENTER);
		textArea.setHighlightedLine(5, false);
		textArea.setHighlightedText("highlight");
		textArea.setTextSource(new VolatileTextSource(
				"Line 1,",
				"Line 2,",
				"Line 3,highlight",
				"Line 4,",
				"Line 5,highlight",
				"Line 6,",
				"Line 7,",
				"Line 8,",
				"Line 9,",
				"Line 10,"
		));
		Dimension size = textArea.getMinimumSize();
		size.height = textArea.getLineHeight() * 9;
		textArea.setMinimumSize(size);
		textArea.setPreferredSize(size);
	}

}
