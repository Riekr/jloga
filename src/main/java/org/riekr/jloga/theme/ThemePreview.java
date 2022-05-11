package org.riekr.jloga.theme;

import javax.swing.*;
import java.awt.*;

import org.riekr.jloga.io.VolatileTextSource;
import org.riekr.jloga.ui.VirtualTextArea;

public class ThemePreview extends JPanel {
	private static final long serialVersionUID = -7755998013962966312L;

	private VirtualTextArea _textArea = new VirtualTextArea(null, "Lorem ipsum", null) {
		private static final long serialVersionUID = 8358979652181379726L;

		@Override
		public void openInPerspective() {}
	};

	public ThemePreview() {
		super(new BorderLayout());
		add(_textArea, BorderLayout.CENTER);
		_textArea.setHighlightedLine(5, false);
		_textArea.setHighlightedText("highlight");
		_textArea.setTextSource(new VolatileTextSource(
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
		Dimension size = _textArea.getMinimumSize();
		size.height = _textArea.getLineHeight() * 11;
		_textArea.setMinimumSize(size);
		_textArea.setPreferredSize(size);
	}

}
