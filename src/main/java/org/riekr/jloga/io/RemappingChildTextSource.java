package org.riekr.jloga.io;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemappingChildTextSource extends ChildTextSource {

	private final Matcher                   _matcher;
	private final Function<Matcher, String> _extractor;

	public RemappingChildTextSource(TextSource tie, Pattern pattern, Function<Matcher, String> extractor) {
		super(tie);
		_matcher = pattern.matcher("");
		_extractor = extractor;
	}

	@Override
	public String getText(int line) {
		String text = super.getText(line);
		_matcher.reset(text);
		if (_matcher.find())
			return _extractor.apply(_matcher);
		return text;
	}
}
