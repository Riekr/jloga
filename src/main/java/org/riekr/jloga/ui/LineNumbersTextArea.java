package org.riekr.jloga.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class LineNumbersTextArea extends JTextArea {
	private static final long serialVersionUID = 3934415485971790115L;

	private int _from = 0, _to = 0, _width = 0;

	public LineNumbersTextArea() {
		setBorder(new EmptyBorder(0, 0, 0, 0));
		setEditable(false);
		setEnabled(false);
	}

	public void reNumerate(int from, int to, int allLinesCount) {
		if (to >= allLinesCount)
			to = allLinesCount - 1;
		int width = (int)Math.log10(allLinesCount + 1);
		if (_from != from || _to != to || _width != width) {
			_from = from;
			_to = to;
			_width = width;
			String fmt = "%0" + (width + 1) + "d ";
			StringBuilder buf = new StringBuilder(String.format(fmt, from++));
			for (int i = from; i <= to; i++)
				buf.append('\n').append(String.format(fmt, i));
			setText(buf.toString());
		}
	}

}
