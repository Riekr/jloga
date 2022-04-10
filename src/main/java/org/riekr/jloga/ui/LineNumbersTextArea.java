package org.riekr.jloga.ui;

import org.riekr.jloga.prefs.Preferences;

import javax.swing.*;

public class LineNumbersTextArea extends JTextArea {
	private static final long serialVersionUID = 3934415485971790115L;

	private int _from, _to, _allLines, _delta;

	public LineNumbersTextArea() {
		setBorder(BorderFactory.createEmptyBorder());
		setEditable(false);
		setEnabled(false);
		Preferences.LINES_FROM1.subscribe(this, (yes) -> {
			int from = _from - _delta;
			int to = _to - _delta;
			int count = _allLines - _delta;
			_delta = yes ? 1 : 0;
			renumerate(from, to, count);
		});
	}

	public void renumerate(int from, int to, int allLinesCount) {
		from += _delta;
		to += _delta;
		allLinesCount += _delta;
		if (to >= allLinesCount)
			to = allLinesCount - 1;
		if (_from != from || _to != to || _allLines != allLinesCount) {
			_from = from;
			_to = to;
			_allLines = allLinesCount;
			int width = (int)Math.log10(allLinesCount + 1);
			String fmt = "%0" + (width + 1) + "d ";
			StringBuilder buf = new StringBuilder(String.format(fmt, from++));
			for (int i = from; i <= to; i++)
				buf.append('\n').append(String.format(fmt, i));
			setText(buf.toString());
		}
	}

}
