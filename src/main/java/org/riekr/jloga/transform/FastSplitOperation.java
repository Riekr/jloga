package org.riekr.jloga.transform;

import static org.riekr.jloga.misc.Constants.EMPTY_STRINGS;

import java.util.ArrayList;
import java.util.function.Function;

public class FastSplitOperation implements Function<String, String[]> {

	public static int count(String s) {
		int max = 0;
		if (s != null && !s.isEmpty()) {
			for (int di = 0, dlen = _DELIMS.length(); di < dlen; di++) {
				char delim = _DELIMS.charAt(di);
				int count = 1;
				for (int si = 0, slen = s.length(); si < slen; si++) {
					if (s.charAt(si) == delim)
						count++;
				}
				if (max < count)
					max = count;
			}
		}
		return max;
	}


	private static final String            _DELIMS = "\t,;:|";
	private final        ArrayList<String> _buffer;
	private              boolean           _detect;
	private              char              _delim;
	private              String            _escapedDelim;
	private              String            _stringDelim;
	private              int               _cols;
	private              int               _line   = 0;

	/** For autodetection */
	public FastSplitOperation() {
		_cols = 0;
		_detect = true;
		_buffer = new ArrayList<>();
	}

	public FastSplitOperation(char delim, int cols) {
		_cols = Math.max(0, cols);
		_buffer = new ArrayList<>(cols);
		setDelim(delim);
	}

	public char getDelim() {
		if (_detect)
			throw new IllegalStateException("Delimiter has not been auto detected yet");
		return _delim;
	}

	public void setDelim(char delim) {
		_detect = false;
		_delim = delim;
		_escapedDelim = "\\" + delim;
		_stringDelim = Character.toString(delim);
	}

	private int find(String s, int start) {
		// skip escaped delimiters
		int pos = s.indexOf(_delim, start);
		while (pos > 0 && s.charAt(pos - 1) == '\\') {
			start = pos + 1;
			pos = s.indexOf(_delim, start);
		}
		return pos;
	}

	private void add(String s) {
		_buffer.add(s.replace(_escapedDelim, _stringDelim));
	}

	private String[] split(String s) {
		// slow, will determine col count but will let check if the count is constant
		_buffer.clear();
		int pos = find(s, 0);
		if (pos == -1)
			add(s);
		else {
			int start = 0;
			do {
				add(s.substring(start, pos));
				start = pos + 1;
			} while ((pos = find(s, start)) != -1);
			if (start <= s.length())
				add(s.substring(start));
		}
		return _buffer.toArray(String[]::new);
	}

	private String[] detect(String s) {
		String[] selectedRec = EMPTY_STRINGS;
		char selectedDelim = 0;
		for (int i = 0, len = _DELIMS.length(); i < len; i++) {
			char delim = _DELIMS.charAt(i);
			setDelim(delim);
			String[] candidate = split(s);
			if (candidate.length > selectedRec.length) {
				selectedRec = candidate;
				selectedDelim = delim;
			}
		}
		if (selectedRec.length == 0)
			throw new RuntimeException("No selected record?!?");
		setDelim(selectedDelim);
		return selectedRec;
	}

	@Override
	public String[] apply(String s) {
		_line++;
		final String[] res;
		if (_cols == 0) {
			if (_detect) {
				res = detect(s);
				_detect = false;
			} else
				res = split(s);
			_cols = res.length;
		} else
			res = split(s);
		if (res.length != _cols)
			throw new IllegalArgumentException("Number of columns changed from " + _cols + " to " + res.length + " at line " + _line);
		return res;
	}
}
