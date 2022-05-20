package org.riekr.jloga.search;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.function.Function.identity;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UniqueSearch extends RegExSearch {

	private final HashSet<String>          _matches = new HashSet<>();
	private final Pattern                  _dateExtractor;
	private final Function<String, String> _dateFormatter;

	public UniqueSearch(@NotNull Pattern pattern, @Nullable Pattern dateExtractor, @Nullable DateTimeFormatter dateParser) {
		super(pattern);
		_dateExtractor = dateExtractor;
		if (dateParser == null)
			_dateFormatter = identity();
		else
			_dateFormatter = (date) -> date == null ? "" : ISO_DATE_TIME.format(dateParser.parse(date));
	}

	@Override
	protected String header(String header) {
		String res = super.header(header);
		if (_dateExtractor != null)
			res = "Timestamp" + DELIMSTR + res;
		return res;
	}

	@Override
	protected Function<String, String> newExtractor() {
		Function<String, String> base = super.newExtractor();
		if (_dateExtractor != null) {
			Matcher dateMatcher = _dateExtractor.matcher("");
			return line -> {
				String res = base.apply(line);
				dateMatcher.reset(line);
				if (dateMatcher.find())
					res = escape(_dateFormatter.apply(dateMatcher.group(1))) + DELIM + res;
				return res;
			};
		}
		return base;
	}

	@Override
	protected Predicate<String> newPredicate(Matcher searchMatcher) {
		if (_groupCount == 0)
			throw new IllegalArgumentException("At least 1 capturing group must be supplied to UniqueSearch");
		Function<String, String> extractor = newExtractor();
		return super.newPredicate(searchMatcher).and((text) -> {
			final String extractedText = extractor.apply(text);
			return _matches.add(extractedText);
		});
	}

	@Override
	public void end(boolean interrupted) {
		super.end(interrupted);
		_matches.clear();
	}
}
