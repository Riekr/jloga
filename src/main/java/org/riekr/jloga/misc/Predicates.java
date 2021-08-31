package org.riekr.jloga.misc;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

public class Predicates {
	private Predicates() {
	}

	@SafeVarargs
	public static <T> Predicate<T> and(Predicate<T>... predicates) {
		List<Predicate<T>> predicateList = Arrays.stream(predicates).filter(Objects::nonNull).collect(toList());
		if (predicateList.isEmpty())
			return (t) -> true;
		Predicate<T> res = predicateList.remove(0);
		for (Predicate<T> predicate : predicateList)
			res = res.and(predicate);
		return res;
	}
}
