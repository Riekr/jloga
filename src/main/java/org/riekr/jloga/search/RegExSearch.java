package org.riekr.jloga.search;

import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.io.ChildTextSource;
import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.RemappingChildTextSource;
import org.riekr.jloga.io.RemappingChildTextSourceWithHeader;
import org.riekr.jloga.io.TextSource;

public class RegExSearch implements SearchPredicate {

	protected static final char   DELIM    = '|';
	protected static final String DELIMSTR = "|";
	protected static final String DELIMESC = "\\|";

	protected final Pattern           _pattern;
	protected       int               _groupCount;
	protected       Predicate<String> _searchPredicate;

	private ChildTextSource _childTextSource;

	public RegExSearch(@NotNull Pattern pattern) {
		_pattern = pattern;
	}

	protected Predicate<String> newPredicate(Matcher searchMatcher) {
		return (text) -> {
			searchMatcher.reset(text);
			return searchMatcher.find();
		};
	}

	protected Function<String, String> newExtractor() {
		final Matcher matcher = _pattern.matcher("");
		final Function<String, String> extract;
		if (_groupCount == 1)
			extract = line -> matcher.group(1);
		else
			extract = new Function<>() {
				private final StringBuilder _buff = new StringBuilder();

				@Override
				public String apply(String line) {
					_buff.replace(0, _buff.length(), requireNonNullElse(matcher.group(1), ""));
					for (int i = 2; i <= _groupCount; i++)
						_buff.append(DELIM).append(escape(matcher.group(i)));
					return _buff.toString();
				}
			};
		return (line) -> {
			matcher.reset(line);
			return matcher.find() ? extract.apply(line) : line;
		};
	}

	protected String escape(String str) {
		return str.replace(DELIMSTR, DELIMESC);
	}

	protected String header(String header) {
		return header;
	}

	@Override
	public FilteredTextSource start(TextSource master) {
		if (_childTextSource != null)
			throw new IllegalStateException("RegExSearch already started");
		Matcher searchMatcher = _pattern.matcher("");
		_groupCount = searchMatcher.groupCount();
		_searchPredicate = newPredicate(searchMatcher);
		if (_groupCount == 0)
			_childTextSource = new ChildTextSource(master);
		else {
			Supplier<Function<String, String>> supplier = ThreadLocal.withInitial(this::newExtractor)::get;
			if (_groupCount == 1)
				_childTextSource = new RemappingChildTextSource(master, supplier);
			else {
				String header = range(1, _groupCount + 1).mapToObj((g) -> "Group " + g).collect(joining(DELIMSTR));
				_childTextSource = new RemappingChildTextSourceWithHeader(master, supplier, header(header));
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
	public void end(boolean interrupted) {
		if (_childTextSource == null)
			throw new IllegalStateException("RegExSearch already finished");
		_childTextSource = null;
	}
}
