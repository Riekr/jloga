package org.riekr.jloga.transform;

public interface Transformer {

	Transformer IDENTITY = (col, val) -> val;

	Transformer UNWRAP_QUOTES = (col, val) -> {
		if (val.startsWith("\"") && val.endsWith("\""))
			return val.substring(1, val.length() - 1).replace("\\\"", "\"");
		return val;
	};

	String apply(int col, String val);

	default Transformer andThen(Transformer other) {
		return (col, val) -> other.apply(col, this.apply(col, val));
	}
}
