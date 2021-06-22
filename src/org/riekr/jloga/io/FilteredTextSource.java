package org.riekr.jloga.io;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FilteredTextSource implements TextSource {

	private final TextSource _tie;
	private final Map<Integer, Integer> _lines = new HashMap<>();

	public FilteredTextSource(TextSource tie) {
		_tie = tie;
	}

	public void addLine(int line) {
		_lines.put(_lines.size(), line);
	}

	@Override
	public String getText(int line) throws ExecutionException, InterruptedException {
		Integer tieLine = _lines.get(line);
		if (tieLine != null)
			return _tie.getText(tieLine);
		return "";
	}

	@Override
	public int getLineCount() {
		return _lines.size();
	}

	public Integer getSrcLine(int line) {
		return _lines.get(line);
	}
}
