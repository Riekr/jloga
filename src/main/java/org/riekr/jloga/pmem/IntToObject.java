package org.riekr.jloga.pmem;

import java.io.Serializable;
import java.util.Objects;

public final class IntToObject<T> implements Serializable {
	private static final long serialVersionUID = 8021199321369057110L;

	public final int tag;
	public final T   value;

	public IntToObject(int tag, T value) {
		this.tag = tag;
		this.value = value;
	}

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
