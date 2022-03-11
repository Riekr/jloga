package org.riekr.jloga.io;

import java.util.concurrent.ExecutionException;
import java.util.function.IntConsumer;

import org.riekr.jloga.pmem.PagedIntToObjList;
import org.riekr.jloga.react.IntBehaviourSubject;
import org.riekr.jloga.react.Unsubscribable;

public class TempTextSource implements FilteredTextSource {

	private final PagedIntToObjList<String> _data             = new PagedIntToObjList<>(10000);
	private final IntBehaviourSubject       _lineCountSubject = new IntBehaviourSubject();

	@Override
	public String getText(int line) throws ExecutionException, InterruptedException {
		if (line < _data.size())
			return _data.getValue(line);
		return "";
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
