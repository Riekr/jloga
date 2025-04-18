package org.riekr.jloga.misc;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class Predicates {
	private Predicates() {}

	@SafeVarargs
	public static <T> Predicate<T> and(Predicate<T>... predicates) {
		List<Predicate<T>> predicateList = Arrays.stream(predicates).filter(Objects::nonNull).collect(toList());
		if (predicateList.isEmpty())
			return (t) -> true;
		Predicate<T> res = predicateList.removeFirst();
		for (Predicate<T> predicate : predicateList)
			res = res.and(predicate);
		return res;
	}

	@Contract(pure = true)
	@NotNull
	public static Supplier<Predicate<String>> supplyFind(@NotNull Pattern pat, boolean negate) {
		return () -> {
			Matcher matcher = pat.matcher("");
			Predicate<String> res = (text) -> {
				matcher.reset(text);
				return matcher.find();
			};
			return negate ? res.negate() : res;
		};
	}

	public static Supplier<Predicate<String>> supplyContains(@NotNull String pattern, boolean caseInsensitive, boolean negate) {
		return () -> {
			Predicate<String> res;
			if (caseInsensitive) {
				String upperPattern = pattern.toUpperCase(Locale.ROOT);
				res = (text) -> text.toUpperCase(Locale.ROOT).contains(upperPattern);
			} else
				res = (text) -> text.contains(pattern);
			return negate ? res.negate() : res;
		};
	}
}
