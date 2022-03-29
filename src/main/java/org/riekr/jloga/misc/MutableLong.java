package org.riekr.jloga.misc;

import java.util.function.LongSupplier;

public final class MutableLong implements LongSupplier {
	public long value;

	@Override public String toString() {
		return Long.toString(value);
	}

	@Override
	public long getAsLong() {
		return value;
	}
}
