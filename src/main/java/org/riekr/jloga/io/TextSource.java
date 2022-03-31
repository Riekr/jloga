package org.riekr.jloga.io;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Reader;
import java.util.Iterator;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongSupplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.misc.MutableInt;
import org.riekr.jloga.react.Unsubscribable;
import org.riekr.jloga.search.SearchException;
import org.riekr.jloga.search.SearchPredicate;

public interface TextSource extends Iterable<String> {

	ExecutorService          IO_EXECUTOR      = Executors.newSingleThreadExecutor();
	ScheduledExecutorService MONITOR_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
	ExecutorService          AUX_EXECUTOR     = Executors.newCachedThreadPool();

	default ScheduledFuture<?> monitorProgress(LongSupplier current, long total, ProgressListener progressListener) {
		return monitorProgress(current, () -> total, progressListener);
	}

	default ScheduledFuture<?> monitorProgress(LongSupplier current, LongSupplier total, ProgressListener progressListener) {
		return MONITOR_EXECUTOR.scheduleWithFixedDelay(
				() -> progressListener.onIntermediate(current.getAsLong(), total.getAsLong()),
				200, 200, TimeUnit.MILLISECONDS);
	}

	/**
	 * Runs in a background executor, may be overridden if the source has a performance
	 * constraint running multiple threads concurrently
	 */
	default void enqueueTextRequest(Runnable task) {
		AUX_EXECUTOR.submit(task);
	}

	/** Runs in AWT thread when ready */
	default void requestText(int fromLine, int count, Consumer<Reader> consumer) {
		enqueueTextRequest(() -> {
			try {
				StringsReader reader = new StringsReader(getText(fromLine, Math.min(getLineCount() - fromLine, count)));
				EventQueue.invokeLater(() -> consumer.accept(reader));
			} catch (CancellationException ignored) {
				System.out.println("Text request cancelled");
			} catch (Throwable e) {
				e.printStackTrace(System.err);
			}
		});
	}

	/** May block if other tasks are running, may be overridden to not block */
	default String[] getText(int fromLine, int count) throws ExecutionException, InterruptedException {
		int toLinePlus1 = Math.min(fromLine + count, getLineCount());
		String[] lines = new String[toLinePlus1 - fromLine + 1];
		for (int i = fromLine; i <= toLinePlus1; i++)
			lines[i - fromLine] = getText(i);
		return lines;
	}

	String getText(int line);

	/** May block if indexing is not finished */
	int getLineCount() throws ExecutionException, InterruptedException;

	/** Runs in AWT thread when ready */
	default Future<Integer> requestIntermediateLineCount(IntConsumer consumer) {
		// implementations that support intermediate line count should override this method
		return requestCompleteLineCount(consumer);
	}

	/** Runs in AWT thread when ready */
	default Future<Integer> requestCompleteLineCount(IntConsumer consumer) {
		return AUX_EXECUTOR.submit(() -> {
			int lineCount = getLineCount();
			EventQueue.invokeLater(() -> consumer.accept(lineCount));
			return lineCount;
		});
	}

	/** Once if stable or multiple if supported and loading */
	default Unsubscribable subscribeLineCount(IntConsumer consumer) {
		Future<Integer> future = requestIntermediateLineCount(consumer);
		return () -> future.cancel(false);
	}

	default Future<?> requestSearch(SearchPredicate predicate, ProgressListener progressListener, Consumer<TextSource> consumer, Consumer<Throwable> onError) {
		AtomicReference<Future<?>> resRef = new AtomicReference<>();
		BooleanSupplier running = () -> {
			Future<?> future = resRef.get();
			return future == null || !future.isCancelled();
		};
		Future<?> res = AUX_EXECUTOR.submit(() -> {
			try {
				FilteredTextSource searchResult = predicate.start(this);
				EventQueue.invokeLater(() -> consumer.accept(searchResult));
				search(predicate, searchResult, progressListener, running);
				searchResult.complete();
			} catch (SearchException e) {
				if (e.userHasAlreadyBeenNotified.compareAndSet(false, true))
					onError.accept(e);
			} catch (Throwable e) {
				e.printStackTrace(System.err);
				onError.accept(e);
			}
		});
		resRef.set(res);
		return res;
	}

	default void search(SearchPredicate predicate, FilteredTextSource out, ProgressListener progressListener, BooleanSupplier running) throws ExecutionException, InterruptedException {
		// dispatchLineCount called here to take advantage of 200ms scheduling of global progressbar update
		ProgressListener fullProgressListener = progressListener.andThen(out::dispatchLineCount);
		long start = System.currentTimeMillis();
		final int lineCount = getLineCount();
		final MutableInt line = new MutableInt();
		ScheduledFuture<?> updateTask = monitorProgress(line, lineCount, fullProgressListener);
		try {
			for (line.value = 0; line.value <= getLineCount() && running.getAsBoolean(); line.value++)
				predicate.verify(line.value, getText(line.value));
		} finally {
			predicate.end(!running.getAsBoolean());
			updateTask.cancel(false);
			fullProgressListener.onProgressChanged(lineCount, lineCount);
			System.out.println("Search finished in " + (System.currentTimeMillis() - start) + "ms");
		}
	}

	default Integer getSrcLine(int line) {
		return line;
	}

	default void requestSave(File file, ProgressListener progressListener) {
		IO_EXECUTOR.execute(() -> {
			try {
				final int lineCount = getLineCount();
				final MutableInt ln = new MutableInt();
				ScheduledFuture<?> updateTask = monitorProgress(ln, lineCount, progressListener);
				try (BufferedWriter w = new BufferedWriter(new FileWriter(file, false))) {
					for (ln.value = 0; ln.value < lineCount; ln.value++) {
						w.write(getText(ln.value));
						w.newLine();
					}
				} finally {
					updateTask.cancel(false);
					progressListener.onProgressChanged(lineCount, lineCount);
				}
			} catch (Throwable e) {
				e.printStackTrace(System.err);
				if (file.isFile() && !file.delete())
					System.err.println("Unable to delete " + file);
			}
		});
	}

	@NotNull
	@Override
	default Iterator<String> iterator() {
		try {
			return new RangeIterator<>(0, getLineCount(), this::getText);
		} catch (ExecutionException | InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	default Iterator<String> iterator(int fromInclusive, int toExclusive) {
		try {
			return new RangeIterator<>(fromInclusive, Math.min(getLineCount(), toExclusive), this::getText);
		} catch (ExecutionException | InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	default Stream<String> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	default void requestStream(Consumer<Stream<String>> streamConsumer) {
		IO_EXECUTOR.execute(() -> streamConsumer.accept(StreamSupport.stream(spliterator(), false)));
	}

	default void onClose() {}

	default boolean isIndexing() {return false;}

	default boolean mayHaveTabularData() {return false;}

}
