package org.riekr.jloga.io;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import org.riekr.jloga.react.Unsubscribable;

public class RemappingChildTextSourceWithHeader extends RemappingChildTextSource {

	private final String _header;

	public RemappingChildTextSourceWithHeader(TextSource tie, Supplier<Function<String, String>> extractor, String header) {
		super(tie, extractor);
		_header = requireNonNull(header);
	}

	@Override
	public int getLineCount() throws ExecutionException, InterruptedException {
		return super.getLineCount() + 1;
	}

	@Override
	public Future<Integer> requestIntermediateLineCount(IntConsumer consumer) {
		return super.requestIntermediateLineCount((lc) -> consumer.accept(lc + 1));
	}

	@Override
	public Unsubscribable subscribeLineCount(IntConsumer consumer) {
		return super.subscribeLineCount((lc) -> consumer.accept(lc + 1));
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
