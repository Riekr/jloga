package org.riekr.jloga.io;

import java.awt.*;
import java.io.Reader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.riekr.jloga.pmem.PagedIntToObjList;
import org.riekr.jloga.react.IntBehaviourSubject;
import org.riekr.jloga.react.Unsubscribable;

public class TempTextSource implements FilteredTextSource {

	private final PagedIntToObjList<String> _data             = new PagedIntToObjList<>(10000);
	private final IntBehaviourSubject       _lineCountSubject = new IntBehaviourSubject();

	@Override
	public String getText(int line) {
		if (line < _data.size())
			return _data.getValue(line);
		return "";
	}

	@Override
	public String[] getText(int fromLine, int count) {
		int toLinePlus1 = Math.min(fromLine + count, Math.toIntExact(_data.size()));
		String[] lines = new String[toLinePlus1 - fromLine + 1];
		for (int i = fromLine; i <= toLinePlus1; i++)
			lines[i - fromLine] = getText(i);
		return lines;
	}

	@Override
	public void requestText(int fromLine, int count, Consumer<Reader> consumer) {
		if (_data.isSealed()) {
			enqueueTextRequest(() -> {
				StringsReader reader = new StringsReader(getText(fromLine, Math.min(Math.toIntExact(_data.size()) - fromLine, count)));
				EventQueue.invokeLater(() -> consumer.accept(reader));
			});
		} else {
			AtomicReference<Runnable> unsubscribe = new AtomicReference<>(() -> {});
			unsubscribe.set(_lineCountSubject.subscribe(lastLine -> {
				int toLine = Math.min(fromLine + count, Math.toIntExact(_data.size()));
				if (fromLine < lastLine) {
					StringsReader reader = new StringsReader(getText(fromLine, toLine - fromLine));
					EventQueue.invokeLater(() -> consumer.accept(reader));
					if (toLine >= lastLine) {
						unsubscribe.get().run();
						return;
					}
				}
				if (_data.isSealed())
					unsubscribe.get().run();
			})::unsubscribe);
		}
	}

	@Override
	public synchronized int getLineCount() throws ExecutionException, InterruptedException {
		if (!_data.isSealed())
			wait();
		return Math.toIntExact(_data.size());
	}

	@Override
	public Unsubscribable requestLineCount(IntConsumer consumer) {
		return _lineCountSubject.subscribe(consumer::accept);
	}

	@Override
	public Integer getSrcLine(int line) {
		if (line < _data.size())
			return _data.getTag(line);
		return _data.getTag(Math.toIntExact(_data.size() - 1));
	}

	@Override
	public void onClose() {
		_data.close();
	}

	@Override
	public void dispatchLineCount() {
		_lineCountSubject.next(Math.toIntExact(_data.size()));
	}

	@Override
	public synchronized void complete() {
		_data.seal();
		_lineCountSubject.last(Math.toIntExact(_data.size()));
		notifyAll();
	}

	public void addLine(int origSrcLine, String text) {
		_data.add(origSrcLine, text);
	}
}
