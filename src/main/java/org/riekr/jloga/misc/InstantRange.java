package org.riekr.jloga.misc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.function.Predicate;

public class InstantRange {

	private InstantRange() {
	}

	@NotNull
	public static Predicate<Instant> from(@Nullable Instant fromInclusive, @Nullable Instant toInclusive) {
		if (fromInclusive == null && toInclusive == null)
			return (i) -> true;
		if (fromInclusive == null) {
			return new Predicate<>() {
				private boolean _valid = true;

				@Override
				public boolean test(Instant instant) {
					if (_valid && instant != null && instant.compareTo(toInclusive) > 0)
						_valid = false;
					return _valid;
				}
			};
		}
		if (toInclusive == null) {
			return new Predicate<>() {
				private boolean _valid = false;

				@Override
				public boolean test(Instant instant) {
					if (!_valid && instant != null && instant.compareTo(fromInclusive) >= 0)
						_valid = true;
					return _valid;
				}
			};
		}
		return from(fromInclusive, null).and(from(null, toInclusive));
	}
}
