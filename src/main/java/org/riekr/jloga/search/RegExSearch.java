package org.riekr.jloga.search;

import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.io.ChildTextSource;
import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.RemappingChildTextSource;
import org.riekr.jloga.io.RemappingChildTextSourceWithHeader;
import org.riekr.jloga.io.TextSource;

public class RegExSearch implements SearchPredicate {

	protected final Pattern           _pattern;
	protected final int               _groupCount;
	protected final Predicate<String> _searchPredicate;

	private ChildTextSource _childTextSource;

	public RegExSearch(@NotNull Pattern pattern) {
		_pattern = pattern;
		Matcher searchMatcher = pattern.matcher("");
		_groupCount = searchMatcher.groupCount();
		_searchPredicate = newPredicate(searchMatcher);
	}

	protected Predicate<String> newPredicate(Matcher searchMatcher) {
		return (text) -> {
			searchMatcher.reset(text);
			return searchMatcher.find();
		};
	}

	protected Function<Matcher, String> newExtractor() {
		if (_groupCount == 1)
			return (matcher) -> matcher.group(1);
		return new Function<>() {
			private final StringBuilder _buff = new StringBuilder();

			@Override
			public String apply(Matcher matcher) {
				_buff.replace(0, _buff.length(), matcher.group(1));
				for (int i = 2; i <= _groupCount; i++)
					_buff.append(',').append(matcher.group(i).replaceAll("([,\\\\])", "\\$1"));
				return _buff.toString();
			}
		};
	}

	@Override
	public FilteredTextSource start(TextSource master) {
		if (_childTextSource != null)
			throw new IllegalStateException("RegExSearch already started");
		if (_groupCount == 0)
			_childTextSource = new ChildTextSource(master);
		else {
			if (_groupCount == 1)
				_childTextSource = new RemappingChildTextSource(master, _pattern, newExtractor());
			else {
				String header = range(1, _groupCount + 1).mapToObj((g) -> "Group " + g).collect(joining(","));
				_childTextSource = new RemappingChildTextSourceWithHeader(master, _pattern, newExtractor(), header);
			}
		}
		return _childTextSource;
	}

	@Override
	public void verify(int line, String text) {
		if (_searchPredicate.test(text))
			_childTextSource.addLine(line);
	}

	@Override
	public void end() {
		if (_childTextSource == null)
			throw new IllegalStateException("RegExSearch already finished");
		_childTextSource = null;
	}
}
