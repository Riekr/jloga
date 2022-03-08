package org.riekr.jloga.transform;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class FastSplitOperation implements Function<String, String[]> {

	private static final String _TSV = "\\t";
	private static final String _CSV = ",";

	private @Nullable String _split;
	private           int    _cols;
	private           int    _line = 0;

	/** For autodetection */
	public FastSplitOperation() {
		this(null, 0);
	}

	public FastSplitOperation(@Nullable String split, int cols) {
		_split = split;
		_cols = Math.max(0, cols);
	}

	@Override
	public String[] apply(String s) {
		_line++;
		String[] res;
		if (_split == null) {
			res = s.split(_split = _TSV);
			String[] t = s.split(_CSV);
			if (t.length > res.length) {
				res = t;
				_split = _CSV;
			}
		} else
			res = s.split(_split);
		if (_cols == 0)
			_cols = res.length;
		else if (res.length != _cols)
			throw new IllegalStateException("Number of columns changed from " + _cols + " to " + res.length + " at line " + _line);
		return res;
	}
}
