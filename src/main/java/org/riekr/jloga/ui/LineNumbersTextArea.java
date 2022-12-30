package org.riekr.jloga.ui;

import javax.swing.*;

import org.riekr.jloga.prefs.Preferences;

public class LineNumbersTextArea extends JTextArea {
	private static final long serialVersionUID = 3934415485971790115L;

	private int _from, _to, _allLines, _width, _delta;

	public LineNumbersTextArea() {
		setBorder(BorderFactory.createEmptyBorder());
		setEditable(false);
		setEnabled(false);
		_delta = Preferences.LINES_FROM1.get() ? 1 : 0;
		Preferences.LINES_FROM1.subscribe(this, (yes) -> {
			int newDelta = yes ? 1 : 0;
			if (_delta != newDelta) {
				int from = _from - _delta;
				int to = _to - _delta;
				int count = _allLines - _delta;
				_delta = newDelta;
				renumerate(from, to, count);
			}
		});
	}

	public void renumerate(int from, int to, int allLinesCount) {
		from += _delta;
		to += _delta;
		allLinesCount += _delta;
		if (to >= allLinesCount)
			to = allLinesCount - 1;
		int width = _allLines == allLinesCount ? _width : (int)Math.log10(allLinesCount + 1);
		if (_from != from || _to != to || _width != width) {
			_from = from;
			_to = to;
			_allLines = allLinesCount;
			_width = width;
			String fmt = "%0" + (width + 1) + "d ";
			StringBuilder buf = new StringBuilder(String.format(fmt, from++));
			for (int i = from; i <= to; i++)
				buf.append('\n').append(String.format(fmt, i));
			setText(buf.toString());
		}
	}

}
