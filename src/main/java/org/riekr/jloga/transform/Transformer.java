package org.riekr.jloga.transform;

public interface Transformer {

	Transformer IDENTITY = (col, val) -> val;

	Transformer UNWRAP_QUOTES = (col, val) -> {
		if (val.startsWith("\"") && val.endsWith("\""))
			return val.substring(1, val.length() - 1).replace("\\\"", "\"");
		return val;
	};

	String apply(int col, String val);

	default String[] apply(String... vals) {
		String[] res = new String[vals.length];
		for (int i = 0; i < vals.length; i++)
			res[i] = apply(i, vals[i]);
		return res;
	}

	default Transformer andThen(Transformer other) {
		return (col, val) -> other.apply(col, this.apply(col, val));
	}
}
