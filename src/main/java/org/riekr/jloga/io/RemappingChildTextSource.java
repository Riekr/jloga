package org.riekr.jloga.io;

import java.util.function.Function;
import java.util.function.Supplier;

public class RemappingChildTextSource extends ChildTextSource {

	private final Supplier<Function<String, String>> _extractor;

	public RemappingChildTextSource(TextSource tie, Supplier<Function<String, String>> extractor) {
		super(tie);
		_extractor = extractor;
	}

	@Override
	public String getText(int line) {
		return _extractor.get().apply(super.getText(line));
	}
}
