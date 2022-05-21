package org.riekr.jloga.io;

import static org.riekr.jloga.pmem.PagedIntToObjList.newPagedIntToStringList;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.riekr.jloga.pmem.PagedIntToObjList;
import org.riekr.jloga.react.IntBehaviourSubject;
import org.riekr.jloga.react.Observer;
import org.riekr.jloga.react.Unsubscribable;
import org.riekr.jloga.utils.CancellableFuture;
import org.riekr.jloga.utils.TextUtils;

public class TempTextSource implements FilteredTextSource {

	private final PagedIntToObjList<String> _data             = newPagedIntToStringList();
	private final IntBehaviourSubject       _lineCountSubject = new IntBehaviourSubject();
	private       LinkedList<Section>       _sectionList;

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
	public Future<?> requestText(int fromLine, int count, Consumer<String> consumer) {

		if (_data.isSealed()) {
			return defaultAsyncIO(() -> {
				String text = TextUtils.toString(getText(fromLine, Math.min(Math.toIntExact(_data.size()) - fromLine, count)), count);
				EventQueue.invokeLater(() -> consumer.accept(text));
			});
		}

		AtomicReference<Runnable> unsubscribe = new AtomicReference<>(() -> {});
		unsubscribe.set(_lineCountSubject.subscribe(lastLine -> {
			int toLine = Math.min(fromLine + count, Math.toIntExact(_data.size()));
			if (fromLine < lastLine) {
				String text = TextUtils.toString(getText(fromLine, toLine - fromLine), count);
				EventQueue.invokeLater(() -> consumer.accept(text));
				if (toLine >= lastLine) {
					unsubscribe.get().run();
					return;
				}
			}
			if (_data.isSealed())
				unsubscribe.get().run();
		})::unsubscribe);
		return new CancellableFuture(unsubscribe.get());
	}

	@Override
	public synchronized int getLineCount() throws ExecutionException, InterruptedException {
		if (!_data.isSealed())
			wait();
		return Math.toIntExact(_data.size());
	}

	@Override
	public Future<Integer> requestIntermediateLineCount(IntConsumer consumer) {
		return _lineCountSubject.once(Observer.async(consumer::accept));
	}

	@Override
	public Unsubscribable subscribeLineCount(IntConsumer consumer) {
		return _lineCountSubject.subscribe(consumer::accept);
	}

	@Override
	public Integer getSrcLine(int line) {
		if (line < _data.size())
			return _data.getTag(line);
		return _data.getTag(Math.toIntExact(_data.size() - 1));
	}

	@Override
	public void close() {
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

	public int pages() {
		return _data.pages();
	}

	@Override
	public List<Section> sections() {
		return _sectionList;
	}

	public synchronized void markSection(String title, boolean excludeStart) {
		int from = _sectionList == null || _sectionList.isEmpty() ? 0 : _sectionList.getLast().to;
		int to = Math.toIntExact(_data.size());
		if (excludeStart) {
			from++;
			to--;
		}
		Section section = new Section(title, from, to);
		if (_sectionList == null)
			_sectionList = new LinkedList<>();
		_sectionList.add(section);
	}
}
