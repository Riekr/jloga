package org.riekr.jloga.search;

import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.io.ChildTextSource;
import org.riekr.jloga.io.FilteredTextSource;
import org.riekr.jloga.io.TextSource;

public class RegExExtract implements SearchPredicate {

	private final Pattern _pattern;

	private ChildTextSource _childTextSource;
	private Matcher         _searchMatcher;

	public RegExExtract(@NotNull Pattern pattern) {
		_pattern = pattern;
		_searchMatcher = pattern.matcher("");
	}

	@Override
	public FilteredTextSource start(TextSource master) {
		if (_childTextSource != null)
			throw new IllegalStateException("RegExExtract already started");
		_childTextSource = new ChildTextSource(master) {
			private final Matcher _viewMatcher = _pattern.matcher("");
			private final Supplier<String> _extractor;
			private final StringBuilder _buff = new StringBuilder();

			{
				int groupCount = _viewMatcher.groupCount();
				if (groupCount == 1)
					_extractor = () -> _viewMatcher.group(1);
				else {
					_extractor = () -> {
						_buff.replace(0, _buff.length(), _viewMatcher.group(1));
						for (int i = 2; i <= groupCount; i++)
							_buff.append(',').append(_viewMatcher.group(i).replaceAll("([,\\\\])", "\\$1"));
						return _buff.toString();
					};
				}
			}

			@Override
			public String getText(int line) throws ExecutionException, InterruptedException {
				String text = super.getText(line);
				_viewMatcher.reset(text);
				if (_viewMatcher.find())
					return _extractor.get();
				return text;
			}
		};
		return _childTextSource;
	}

	@Override
	public void verify(int line, String text) {
		_searchMatcher.reset(text);
		if (_searchMatcher.find())
			_childTextSource.addLine(line);
	}

	@Override
	public void end() {
		if (_childTextSource == null)
			throw new IllegalStateException("RegExExtract already finished");
		_childTextSource = null;
		_searchMatcher = null;
		SearchPredicate.super.end();
	}
}
