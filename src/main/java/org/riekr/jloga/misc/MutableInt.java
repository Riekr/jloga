package org.riekr.jloga.misc;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

public final class MutableInt implements IntSupplier, LongSupplier {
	public int value;

	@Override public String toString() {
		return Integer.toString(value);
	}

	@Override
	public int getAsInt() {
		return value;
	}

	@Override
	public long getAsLong() {
		return value;
	}
}
