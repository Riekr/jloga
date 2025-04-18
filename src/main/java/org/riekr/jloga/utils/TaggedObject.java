package org.riekr.jloga.utils;

import java.io.Serial;
import java.io.Serializable;

public record TaggedObject<T, V>(
		T tag,
		V value
) implements Serializable {
	@Serial private static final long serialVersionUID = -4879529322166442292L;
}
