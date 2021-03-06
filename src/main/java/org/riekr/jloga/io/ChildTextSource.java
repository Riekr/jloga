package org.riekr.jloga.io;

import static org.riekr.jloga.utils.AsyncOperations.asyncTask;

import java.awt.*;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.riekr.jloga.react.IntBehaviourSubject;
import org.riekr.jloga.react.Unsubscribable;
import org.riekr.jloga.utils.TextUtils;

public class ChildTextSource implements FilteredTextSource {

	private static class Range {
		final int from;
		int to;

		private Range(int from) {
			this.from = from;
			this.to = from;
		}

		public Integer getSrcLine(int base, int line) {
			int offset = line - base;
			int res = from + offset;
			return res > to ? null : res;
		}
	}

	private final TextSource              _tie;
	private final TreeMap<Integer, Range> _lines = new TreeMap<>();

	private volatile int                 _lineCount        = 0;
	private final    IntBehaviourSubject _lineCountSubject = new IntBehaviourSubject();
	private final    CountDownLatch      _latch            = new CountDownLatch(1);

	private int _lastLine = -1;

	public ChildTextSource(TextSource tie) {
		_tie = tie;
	}

	public synchronized void addLine(int line) {
		if (line > _lastLine)
			_lastLine = line;
		Map.Entry<Integer, Range> entry = _lines.floorEntry(line);
		if (entry == null) {
			_lines.put(_lineCount++, new Range(line));
			_lineCountSubject.next(_lineCount);
			return;
		}
		Range range = entry.getValue();
		Integer tieLine = range.getSrcLine(entry.getKey(), line - 1);
		if (tieLine == null) {
			_lines.put(_lineCount++, new Range(line));
			_lineCountSubject.next(_lineCount);
			return;
		}
		range.to++;
		_lineCount++;
		_lineCountSubject.next(_lineCount);
	}

	public void dispatchLineCount() {
		_lineCountSubject.next(_lineCount);
	}

	public void complete() {
		_lineCountSubject.last();
		_latch.countDown();
	}

	@Override
	public Future<Integer> requestIntermediateLineCount(IntConsumer consumer) {
		return _lineCountSubject.once(consumer::accept);
	}

	@Override
	public Unsubscribable subscribeLineCount(IntConsumer consumer) {
		return _lineCountSubject.subscribe(consumer::accept);
	}

	@Override
	public String getText(int line) {
		Integer tieLine = getSrcLine(line);
		if (tieLine != null)
			return _tie.getText(tieLine);
		return "";
	}

	@Override
	public int getLineCount() throws ExecutionException, InterruptedException {
		_tie.getLineCount(); // wait for indexing
		_latch.await(); // wait for population
		return _lineCount;
	}

	public Integer getSrcLine(int line) {
		Map.Entry<Integer, Range> entry = _lines.floorEntry(line);
		if (entry == null)
			return null;
		return entry.getValue().getSrcLine(entry.getKey(), line);
	}

	@Override
	public String[] getText(int fromLine, int count) throws ExecutionException, InterruptedException {
		int toLinePlus1 = Math.min(fromLine + count, _lineCount);
		String[] lines = new String[Math.max(toLinePlus1 - fromLine + 1, 0)];
		for (int i = 0; i < lines.length; i++)
			lines[i] = getText(i + fromLine);
		return lines;
	}

	@Override
	public Future<?> defaultAsyncIO(Runnable task) {
		// real i/o is done by the tie!
		return asyncTask(task);
	}

	@Override
	public Future<?> requestText(int fromLine, int count, Consumer<String> consumer) {
		return defaultAsyncIO(() -> {
			try {
				String text = TextUtils.toString(getText(fromLine, Math.min(_lineCount - fromLine, count)), count);
				EventQueue.invokeLater(() -> consumer.accept(text));
			} catch (CancellationException ignored) {
				System.out.println("Text request cancelled");
			} catch (Throwable e) {
				e.printStackTrace(System.err);
			}
		});
	}
}
