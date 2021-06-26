package org.riekr.jloga.io;

import org.jetbrains.annotations.NotNull;
import org.riekr.jloga.react.Unsubscribable;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Reader;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface TextSource {

	ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
	AtomicReference<Future<?>> TEXT_FUTURE = new AtomicReference<>();

	private static void executeRequestText(Runnable task) {
		Future<?> oldFuture = TEXT_FUTURE.getAndSet(EXECUTOR.submit(task));
		if (oldFuture != null)
			oldFuture.cancel(true);
	}

	default void requestText(int fromLine, int count, Consumer<Reader> consumer) {
		executeRequestText(() -> {
			try {
				StringsReader reader = new StringsReader(getText(fromLine, count));
				EventQueue.invokeLater(() -> consumer.accept(reader));
			} catch (InterruptedException ignored) {
			} catch (Throwable e) {
				e.printStackTrace(System.err);
			}
		});
	}

	default String[] getText(int fromLine, int count) throws ExecutionException, InterruptedException {
		int toLinePlus1 = fromLine + Math.min(count, getLineCount());
		String[] lines = new String[toLinePlus1 - fromLine + 1];
		for (int i = fromLine; i <= toLinePlus1; i++)
			lines[i - fromLine] = getText(i);
		return lines;
	}

	String getText(int line) throws ExecutionException, InterruptedException;

	int getLineCount() throws ExecutionException, InterruptedException;

	default Unsubscribable requestLineCount(IntConsumer consumer) {
		Future<?> future = EXECUTOR.submit(() -> {
			try {
				int lineCount = getLineCount();
				EventQueue.invokeLater(() -> consumer.accept(lineCount));
			} catch (Throwable e) {
				e.printStackTrace(System.err);
			}
		});
		return () -> future.cancel(true);
	}

	default void setIndexingListener(@NotNull ProgressListener indexingListener) {
	}

	default Future<?> requestSearch(Pattern pat, ProgressListener progressListener, Consumer<TextSource> consumer) {
		AtomicReference<Future<?>> resRef = new AtomicReference<>();
		BooleanSupplier running = () -> {
			Future<?> future = resRef.get();
			return future == null || !future.isCancelled();
		};
		Future<?> res = Executors.newSingleThreadExecutor().submit(() -> {
			try {
				FilteredTextSource searchResult = new FilteredTextSource(this);
				EventQueue.invokeLater(() -> consumer.accept(searchResult));
				search(pat, searchResult, progressListener, running);
				searchResult.complete();
			} catch (Throwable e) {
				e.printStackTrace(System.err);
			}
		});
		resRef.set(res);
		return res;
	}

	default Stream<? extends CharSequence> allLines() throws ExecutionException, InterruptedException {
		return IntStream.range(0, getLineCount())
				.mapToObj(line -> {
					try {
						return getText(line);
					} catch (ExecutionException | InterruptedException e) {
						return null;
					}
				})
				.filter(Objects::nonNull);
	}

	default void search(Pattern pat, FilteredTextSource out, ProgressListener progressListener, BooleanSupplier running) throws ExecutionException, InterruptedException {
		// dispatchLineCount called here to take advantage of 200ms scheduling of global progressbar update
		progressListener = progressListener.andThen((pos, of) -> out.dispatchLineCount());
		long start = System.currentTimeMillis();
		int lineCount = getLineCount();
		try {
			Matcher m = pat.matcher("");
			Iterator<? extends CharSequence> i = allLines().iterator();
			int line = 0;
			while (running.getAsBoolean() && i.hasNext()) {
				m.reset(i.next());
				if (m.find())
					out.addLine(line);
				line++;
				progressListener.onProgressChanged(line, lineCount);
			}
		} finally {
			progressListener.onProgressChanged(lineCount, lineCount);
			System.out.println("Search finished in " + (System.currentTimeMillis() - start) + "ms");
		}
	}

	default Integer getSrcLine(int line) {
		return line;
	}

	default void requestSave(File file, ProgressListener listener) {
		EXECUTOR.execute(() -> {
			try {
				int lineCount = getLineCount();
				try (BufferedWriter w = new BufferedWriter(new FileWriter(file, false))) {
					for (int ln = 0; ln < lineCount; ln++) {
						w.write(getText(ln));
						w.newLine();
						listener.onProgressChanged(ln, lineCount);
					}
				} finally {
					listener.onProgressChanged(lineCount, lineCount);
				}
			} catch (Throwable e) {
				e.printStackTrace(System.err);
				if (file.isFile() && !file.delete())
					System.err.println("Unable to delete " + file);
			}
		});
	}

}
