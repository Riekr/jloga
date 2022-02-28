package org.riekr.jloga.search;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.io.ChildTextSource;
import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TextSource;

public class RegExSearch implements SearchPredicate {

	protected final Pattern                   _pattern;
	protected final Function<Matcher, String> _extractor;
	protected final Predicate<String>         _searchMatcher;

	private ChildTextSource _childTextSource;


	public RegExSearch(@NotNull Pattern pattern) {
		_pattern = pattern;
		Matcher searchMatcher = pattern.matcher("");
		_searchMatcher = getPredicate(searchMatcher);
		int groupCount = searchMatcher.groupCount();
		if (groupCount == 1)
			_extractor = (matcher) -> matcher.group(1);
		else {
			_extractor = (matcher) -> {
				StringBuilder _buff = new StringBuilder(matcher.group(1));
				for (int i = 2; i <= groupCount; i++)
					_buff.append(',').append(matcher.group(i).replaceAll("([,\\\\])", "\\$1"));
				return _buff.toString();
			};
		}
	}

	protected Predicate<String> getPredicate(Matcher searchMatcher) {
		return (text) -> {
			searchMatcher.reset(text);
			return searchMatcher.find();
		};
	}

	@Override
	public FilteredTextSource start(TextSource master) {
		if (_childTextSource != null)
			throw new IllegalStateException("RegExSearch already started");
		_childTextSource = new ChildTextSource(master) {
			private final Matcher _viewMatcher = _pattern.matcher("");

			@Override
			public String getText(int line) throws ExecutionException, InterruptedException {
				String text = super.getText(line);
				if (_viewMatcher.find())
					return _extractor.apply(_viewMatcher);
				return text;
			}
		};
		return _childTextSource;
	}

	@Override
	public void verify(int line, String text) {
		if (_searchMatcher.test(text))
			_childTextSource.addLine(line);
	}

	@Override
	public void end() {
		if (_childTextSource == null)
			throw new IllegalStateException("RegExSearch already finished");
		_childTextSource = null;
	}
}
