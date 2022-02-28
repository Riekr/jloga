package org.riekr.jloga.search;

import java.util.HashSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

public class UniqueSearch extends RegExSearch {

	private final HashSet<String> _matches = new HashSet<>();

	public UniqueSearch(@NotNull Pattern pattern) {
		super(pattern);
	}

	@Override
	protected Predicate<String> getPredicate(Matcher searchMatcher) {
		if (searchMatcher.groupCount() == 0)
			throw new IllegalArgumentException("At least 1 capturing group must be supplied to UniqueSearch");
		return super.getPredicate(searchMatcher).and((text) -> {
			final String extractedText = _extractor.apply(searchMatcher);
			return _matches.add(extractedText);
		});
	}

	@Override
	public void end() {
		super.end();
		_matches.clear();
	}
}
