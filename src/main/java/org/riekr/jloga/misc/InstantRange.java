package org.riekr.jloga.misc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.function.Function.identity;

public class InstantRange {

	private InstantRange() {
	}

	private static <T extends Comparable<T>> Predicate<Instant> from(T fromInclusive, Function<Instant, T> mapper) {
		return fromInclusive == null ? null : new Predicate<>() {
			private boolean _valid = false;

			@Override
			public boolean test(Instant instant) {
				if (!_valid && instant != null && fromInclusive.compareTo(mapper.apply(instant)) <= 0)
					_valid = true;
				return _valid;
			}
		};
	}

	private static <T extends Comparable<T>> Predicate<Instant> to(T toInclusive, Function<Instant, T> mapper) {
		return toInclusive == null ? null : new Predicate<>() {
			private boolean _valid = true;

			@Override
			public boolean test(Instant instant) {
				if (_valid && instant != null && toInclusive.compareTo(mapper.apply(instant)) < 0)
					_valid = false;
				return _valid;
			}
		};
	}

	@NotNull
	public static Predicate<Instant> from(@Nullable Instant fromInclusive, @Nullable Instant toInclusive) {
		return Predicates.and(
				from(fromInclusive, identity()),
				to(toInclusive, identity())
		);
	}

	@NotNull
	public static Predicate<Instant> from(@Nullable LocalDate fromDate, @Nullable LocalTime fromTime, @Nullable LocalDate toDate, @Nullable LocalTime toTime) {
		ZoneId zone = ZoneId.of("UTC");
		return Predicates.and(
				from(fromDate, (i) -> LocalDate.from(i.atZone(zone))),
				from(fromTime, (i) -> LocalTime.from(i.atZone(zone))),
				to(toDate, (i) -> LocalDate.from(i.atZone(zone))),
				to(toTime, (i) -> LocalTime.from(i.atZone(zone)))
		);
	}
}
