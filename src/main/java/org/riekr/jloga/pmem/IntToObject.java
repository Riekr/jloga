package org.riekr.jloga.pmem;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public record IntToObject<T>(int tag, T value) implements Serializable {
	@Serial private static final long serialVersionUID = 8021199321369057110L;

	@Override
	public String toString() {
		return "[" + tag + ':' + value + ']';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		IntToObject<?> that = (IntToObject<?>)o;
		if (tag != that.tag)
			return false;
		return Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		// usually tag is a referring line number thus we can safely
		// hash on tag only to improve performance
		return tag;
	}
}
