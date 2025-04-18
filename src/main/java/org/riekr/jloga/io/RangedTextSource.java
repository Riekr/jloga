package org.riekr.jloga.io;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.File;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.react.Unsubscribable;
import org.riekr.jloga.search.SearchPredicate;

public class RangedTextSource implements TextSource {

	public static boolean areCorrelated(TextSource a, TextSource b) {
		while (a instanceof RangedTextSource)
			a = ((RangedTextSource)a)._textSource;
		while (b instanceof RangedTextSource)
			b = ((RangedTextSource)b)._textSource;
		return a == b;
	}

	private final @NotNull TextSource _textSource;
	private final          int        _from, _count;

	public RangedTextSource(@NotNull TextSource textSource, int from, int to) {
		_textSource = textSource;
		_from = from;
		_count = to - from;
	}

	private int from(int from) {
		return _from + from;
	}

	private int count(int from, int count) {
		if (_count < count)
			count = _count;
		count = min(count, _count - from);
		return max(count, 0);
	}

	@Override
	public Future<?> defaultAsyncIO(Runnable task) {
		return _textSource.defaultAsyncIO(task);
	}

	@Override
	public Future<?> requestText(int fromLine, int count, Consumer<String> consumer) {
		return _textSource.requestText(from(fromLine), count(fromLine, count), consumer);
	}

	@Override
	public String[] getText(int fromLine, int count) throws ExecutionException, InterruptedException {
		return _textSource.getText(from(fromLine), count(fromLine, count));
	}

	@Override
	public String getText(int line) {
		return _textSource.getText(from(line));
	}

	@Override
	public int getLineCount() {
		return _count;
	}

	@Override
	public Future<Integer> requestIntermediateLineCount(IntConsumer consumer) {
		return TextSource.super.requestIntermediateLineCount(consumer);
	}

	@Override
	public Future<Integer> requestCompleteLineCount(IntConsumer consumer) {
		return TextSource.super.requestCompleteLineCount(consumer);
	}

	@Override
	public Unsubscribable subscribeLineCount(IntConsumer consumer) {
		consumer.accept(_count);
		return null;
	}

	@Override
	public Future<?> requestSearch(SearchPredicate predicate, ProgressListener progressListener, Consumer<TextSource> consumer, Consumer<Throwable> onError) {
		return TextSource.super.requestSearch(predicate, progressListener, consumer, onError);
	}

	@Override
	public void search(SearchPredicate predicate, FilteredTextSource out, ProgressListener progressListener, BooleanSupplier running) throws ExecutionException, InterruptedException {
		TextSource.super.search(predicate, out, progressListener, running);
	}

	@Override
	public Integer getSrcLine(int line) {
		return _textSource.getSrcLine(from(line));
	}

	@Override
	public void requestSave(File file, ProgressListener progressListener) {
		TextSource.super.requestSave(file, progressListener);
	}

	@Override
	public @NotNull Iterator<String> iterator() {
		return _textSource.iterator(_from, from(_count));
	}

	@Override
	public void forEach(Consumer<? super String> action) {
		TextSource.super.forEach(action);
	}

	@Override
	public Iterator<String> iterator(int fromInclusive, int toExclusive) {
		return _textSource.iterator(from(fromInclusive), min(from(_count), toExclusive));
	}

	@Override
	public Future<?> requestStream(Consumer<Stream<String>> streamConsumer) {
		return TextSource.super.requestStream(streamConsumer);
	}

	@Override
	public boolean isIndexing() {
		return _textSource.isIndexing();
	}

	@Override
	public boolean mayHaveTabularData() {
		return _textSource.mayHaveTabularData();
	}

	@Override
	public Future<?> requestReload(Supplier<ProgressListener> progressListenerSupplier) {return null;}

	@Override
	public void reload(Supplier<ProgressListener> progressListenerSupplier) {}

}
