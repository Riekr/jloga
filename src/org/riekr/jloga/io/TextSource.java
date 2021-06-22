package org.riekr.jloga.io;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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

	default void requestText(int fromLine, int count, Consumer<String> consumer) {
		executeRequestText(() -> {
			try {
				String text = getText(fromLine, count);
				EventQueue.invokeLater(() -> consumer.accept(text));
			} catch (Throwable e) {
				e.printStackTrace(System.err);
			}
		});
	}

	default String getText(int fromLine, int count) throws ExecutionException, InterruptedException {
		StringBuilder buf = new StringBuilder(32768);
		int toLinePlus1 = fromLine + Math.min(count, getLineCount());
		for (int line = fromLine; line < toLinePlus1; line++)
			buf.append(getText(line)).append('\n');
		return buf.toString();
	}

	String getText(int line) throws ExecutionException, InterruptedException;

	int getLineCount() throws ExecutionException, InterruptedException;

	default void requestLineCount(IntConsumer consumer) {
		EXECUTOR.execute(() -> {
			try {
				int lineCount = getLineCount();
				EventQueue.invokeLater(() -> consumer.accept(lineCount));
			} catch (Throwable e) {
				e.printStackTrace(System.err);
			}
		});
	}

	default void setIndexingListener(@NotNull ProgressListener indexingListener) {
	}

	default void requestSearch(Pattern pat, ProgressListener progressListener, AtomicBoolean running, Consumer<TextSource> consumer) {
		EXECUTOR.execute(() -> {
			try {
				TextSource searchResult = search(pat, progressListener, running);
				EventQueue.invokeLater(() -> consumer.accept(searchResult));
			} catch (Throwable e) {
				e.printStackTrace(System.err);
			}
		});
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

	default TextSource search(Pattern pat, ProgressListener progressListener, AtomicBoolean running) throws ExecutionException, InterruptedException {
		long start = System.currentTimeMillis();
		int lineCount = getLineCount();
		try {
			FilteredTextSource res = new FilteredTextSource(this);
			Matcher m = pat.matcher("");
			Iterator<? extends CharSequence> i = allLines().iterator();
			int line = 0;
			while (running.get() && i.hasNext()) {
				m.reset(i.next());
				if (m.find())
					res.addLine(line);
				line++;
				progressListener.onProgressChanged(line, lineCount);
			}
			return res;
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
