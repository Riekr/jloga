package org.riekr.jloga.misc;

import java.util.Objects;

@SuppressWarnings("unused")
@FunctionalInterface
public interface IntObjPredicate<T> {

	boolean test(int i, T o);

	default IntObjPredicate<T> and(IntObjPredicate<? super T> other) {
		Objects.requireNonNull(other);
		return (i, o) -> test(i, o) && other.test(i, o);
	}

	default IntObjPredicate<T> negate() {
		return (i, o) -> !test(i, o);
	}

	default IntObjPredicate<T> or(IntObjPredicate<? super T> other) {
		Objects.requireNonNull(other);
		return (i, o) -> test(i, o) || other.test(i, o);
	}

	@SuppressWarnings("unchecked")
	static <T> IntObjPredicate<T> not(IntObjPredicate<? super T> target) {
		Objects.requireNonNull(target);
		return (IntObjPredicate<T>) target.negate();
	}

}

