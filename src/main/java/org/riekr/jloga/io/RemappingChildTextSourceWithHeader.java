package org.riekr.jloga.io;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.riekr.jloga.react.Unsubscribable;

public class RemappingChildTextSourceWithHeader extends RemappingChildTextSource {

	private final String _header;

	public RemappingChildTextSourceWithHeader(TextSource tie, Pattern pattern, Function<Matcher, String> extractor, String header) {
		super(tie, pattern, extractor);
		_header = Objects.requireNonNull(header);
	}

	@Override
	public int getLineCount() throws ExecutionException, InterruptedException {
		return super.getLineCount() + 1;
	}

	@Override
	public Unsubscribable requestIntermediateLineCount(IntConsumer consumer) {
		return super.requestIntermediateLineCount((lc) -> consumer.accept(lc + 1));
	}

	@Override
	public String getText(int line) {
		return line == 0 ? _header : super.getText(line);
	}

	@Override
	public Integer getSrcLine(int line) {
		return line == 0 ? null : super.getSrcLine(line);
	}

	@Override
	public boolean mayHaveTabularData() {
		return true;
	}
}
